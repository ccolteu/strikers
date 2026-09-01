#!/usr/bin/env python3
"""Project player_b_4.png into six orthographic bank frames.

Green-range isolation + 1px erosion strips chroma fringe. Yaw is a 2D
rotation; roll is X-axis compression with Y held at 1. The result is
stenciled onto a flat #00FF00 canvas at native size.
"""

from __future__ import annotations

import argparse
from pathlib import Path

import cv2
import numpy as np

CHROMA_BGR = (0, 255, 0)  # #00FF00
CANVAS = 256
GREEN_LO = np.array([0, 200, 0], dtype=np.uint8)    # BGR of RGB (0, 200, 0)
GREEN_HI = np.array([100, 255, 100], dtype=np.uint8)  # BGR of RGB (100, 255, 100)
ERODE_KERNEL = np.ones((3, 3), dtype=np.uint8)
DRAWABLE = Path(__file__).resolve().parent / "app/src/main/res/drawable"
BANK_FRAMES = (
    ("player_b_1.png", -12.0, -34.0),
    ("player_b_2.png", -6.0, -22.0),
    ("player_b_3.png", -3.0, -12.0),
    ("player_b_5.png", 3.0, 12.0),
    ("player_b_6.png", 6.0, 22.0),
    ("player_b_7.png", 12.0, 34.0),
)


def isolate_plane(image: np.ndarray) -> np.ndarray:
    """BGRA fighter only. All greens in range become alpha 0; RGB zeroed."""
    green = cv2.inRange(image, GREEN_LO, GREEN_HI)
    plane = cv2.bitwise_not(green)
    plane = cv2.erode(plane, ERODE_KERNEL, iterations=1)
    bgra = np.zeros((image.shape[0], image.shape[1], 4), dtype=np.uint8)
    keep = plane == 255
    bgra[keep, :3] = image[keep]
    bgra[:, :, 3] = plane
    return bgra


def detect_tail_pivot(alpha: np.ndarray) -> tuple[float, float]:
    h, w = alpha.shape[:2]
    fallback = (w * 0.5, float(h - 1))
    ys, xs = np.nonzero(alpha == 255)
    if xs.size == 0:
        return fallback
    max_y = int(ys.max())
    on_row = xs[ys == max_y]
    return (float(on_row.mean()), float(max_y))


def affine_matrix(cx: float, cy: float, yaw_deg: float, roll_deg: float) -> np.ndarray:
    """Scale X by |cos(roll)| about the pivot, then rotate yaw. Y scale is 1."""
    sx = float(abs(np.cos(np.deg2rad(roll_deg))))
    scale = np.array(
        [
            [sx, 0.0, cx * (1.0 - sx)],
            [0.0, 1.0, 0.0],
            [0.0, 0.0, 1.0],
        ],
        dtype=np.float64,
    )
    rotate = np.vstack(
        [cv2.getRotationMatrix2D((cx, cy), yaw_deg, 1.0), [0.0, 0.0, 1.0]],
    )
    return (rotate @ scale)[:2]


def stamp_centered(crop_bgr: np.ndarray, crop_mask: np.ndarray) -> np.ndarray:
    canvas = np.full((CANVAS, CANVAS, 3), CHROMA_BGR, dtype=np.uint8)
    ch, cw = crop_mask.shape[:2]
    if cw < 1 or ch < 1:
        return canvas
    ox = (CANVAS - cw) // 2
    oy = (CANVAS - ch) // 2
    src_x0 = 0
    src_y0 = 0
    if ox < 0:
        src_x0 = -ox
        cw = CANVAS
        ox = 0
    if oy < 0:
        src_y0 = -oy
        ch = CANVAS
        oy = 0
    src_x1 = src_x0 + min(cw, CANVAS - ox)
    src_y1 = src_y0 + min(ch, CANVAS - oy)
    patch_bgr = crop_bgr[src_y0:src_y1, src_x0:src_x1]
    patch_mask = crop_mask[src_y0:src_y1, src_x0:src_x1]
    ph, pw = patch_mask.shape[:2]
    dest = canvas[oy : oy + ph, ox : ox + pw]
    keep = patch_mask == 255
    dest[keep] = patch_bgr[keep]
    return canvas


def warp_bank(
    plane_bgra: np.ndarray,
    yaw_deg: float,
    roll_deg: float,
    pivot: tuple[float, float],
) -> np.ndarray:
    h, w = plane_bgra.shape[:2]
    matrix = affine_matrix(pivot[0], pivot[1], yaw_deg, roll_deg)
    corners = np.array(
        [[0.0, 0.0], [w - 1.0, 0.0], [w - 1.0, h - 1.0], [0.0, h - 1.0]],
        dtype=np.float64,
    )
    ones = np.ones((4, 1), dtype=np.float64)
    mapped = (matrix @ np.hstack([corners, ones]).T).T
    min_x, min_y = mapped[:, 0].min(), mapped[:, 1].min()
    max_x, max_y = mapped[:, 0].max(), mapped[:, 1].max()
    pad = 8.0
    ow = int(np.ceil(max_x - min_x + pad * 2.0))
    oh = int(np.ceil(max_y - min_y + pad * 2.0))
    shifted = matrix.copy()
    shifted[0, 2] += -min_x + pad
    shifted[1, 2] += -min_y + pad
    warped = cv2.warpAffine(
        plane_bgra,
        shifted,
        (max(ow, 1), max(oh, 1)),
        flags=cv2.INTER_NEAREST,
        borderMode=cv2.BORDER_CONSTANT,
        borderValue=(0, 0, 0, 0),
    )
    alpha = warped[:, :, 3]
    ys, xs = np.nonzero(alpha == 255)
    if xs.size == 0:
        return np.full((CANVAS, CANVAS, 3), CHROMA_BGR, dtype=np.uint8)
    x0, x1 = int(xs.min()), int(xs.max())
    y0, y1 = int(ys.min()), int(ys.max())
    return stamp_centered(
        warped[y0 : y1 + 1, x0 : x1 + 1, :3],
        alpha[y0 : y1 + 1, x0 : x1 + 1],
    )


def resolve_source(explicit: Path | None) -> Path:
    candidates = []
    if explicit is not None:
        candidates.append(explicit)
    candidates.extend(
        [
            DRAWABLE / "player_b_4.png",
            Path.cwd() / "player_b_4.png",
            Path.cwd() / "app/src/main/res/drawable/player_b_4.png",
        ]
    )
    for path in candidates:
        if path.is_file():
            return path
    searched = "\n  ".join(str(p) for p in candidates)
    raise FileNotFoundError(f"Could not find player_b_4.png. Looked in:\n  {searched}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate orthographic bank frames from player_b_4.png")
    parser.add_argument("--input", type=Path, default=None, help="Path to player_b_4.png")
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=None,
        help="Directory for player_b_1/2/3/5/6/7.png (defaults to drawable)",
    )
    args = parser.parse_args()

    source = resolve_source(args.input)
    image = cv2.imread(str(source), cv2.IMREAD_COLOR)
    if image is None:
        raise RuntimeError(f"Failed to decode {source}")

    out_dir = args.output_dir if args.output_dir is not None else DRAWABLE
    out_dir.mkdir(parents=True, exist_ok=True)

    plane_bgra = isolate_plane(image)
    pivot = detect_tail_pivot(plane_bgra[:, :, 3])
    print(f"source={source}")
    print(f"size={image.shape[1]}x{image.shape[0]} pivot=({pivot[0]:.1f}, {pivot[1]:.1f})")

    for name, yaw, roll in BANK_FRAMES:
        frame = warp_bank(plane_bgra, yaw, roll, pivot)
        dest = out_dir / name
        if not cv2.imwrite(str(dest), frame):
            raise RuntimeError(f"Failed to write {dest}")
        print(f"wrote {dest}  yaw={yaw:+g}  roll={roll:+g}  sx={abs(np.cos(np.deg2rad(roll))):.3f}")


if __name__ == "__main__":
    main()
