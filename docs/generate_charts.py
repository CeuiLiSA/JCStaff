#!/usr/bin/env python3
"""
Generate charts for APP_SWITCHER_PHYSICS.md documentation.
iOS-style App Switcher physics visualization.
"""

import os
import numpy as np
import matplotlib.pyplot as plt
from pathlib import Path

# Get the directory where this script is located
SCRIPT_DIR = Path(__file__).parent
CHARTS_DIR = SCRIPT_DIR / 'charts'

# Set style for beautiful charts
plt.style.use('default')
plt.rcParams.update({
    'figure.facecolor': '#1a1a2e',
    'axes.facecolor': '#16213e',
    'axes.edgecolor': '#e94560',
    'axes.labelcolor': '#eaeaea',
    'axes.titlecolor': '#ffffff',
    'xtick.color': '#eaeaea',
    'ytick.color': '#eaeaea',
    'grid.color': '#0f3460',
    'grid.alpha': 0.6,
    'text.color': '#eaeaea',
    'font.family': 'sans-serif',
    'font.size': 11,
    'axes.titlesize': 14,
    'axes.labelsize': 12,
    'legend.facecolor': '#16213e',
    'legend.edgecolor': '#e94560',
    'legend.fontsize': 10,
    'figure.dpi': 150,
})

# Colors
CYAN = '#00d9ff'
MAGENTA = '#e94560'
YELLOW = '#ffc857'
GREEN = '#06d6a0'
PURPLE = '#b388ff'
ORANGE = '#ff6b35'

def save_chart(fig, name):
    """Save chart with tight layout."""
    fig.tight_layout()
    filepath = CHARTS_DIR / name
    fig.savefig(filepath, facecolor=fig.get_facecolor(),
                edgecolor='none', bbox_inches='tight', pad_inches=0.2)
    plt.close(fig)
    print(f"  ✓ Generated {name}")


def chart_01_geometric_series():
    """Chart 1: Geometric series convergence for left cards."""
    fig, ax = plt.subplots(figsize=(10, 6))

    basePeek = 0.22
    decay = 0.28
    d = np.linspace(0, 6, 200)

    # Cumulative offset
    offset = basePeek * (1 - decay**d) / (1 - decay)
    limit = basePeek / (1 - decay)

    ax.plot(d, offset, color=CYAN, linewidth=2.5, label='Cumulative offset(d)')
    ax.axhline(y=limit, color=MAGENTA, linestyle='--', linewidth=2,
               label=f'Convergence limit = {limit:.3f}')

    # Mark integer points
    for i in range(5):
        y = basePeek * (1 - decay**i) / (1 - decay)
        ax.plot(i, y, 'o', color=YELLOW, markersize=10, zorder=5)
        ax.annotate(f'd={i}\n{y:.3f}', (i, y), textcoords="offset points",
                   xytext=(10, 10), fontsize=9, color=YELLOW)

    ax.set_xlabel('Depth d = -relPos (left card count)')
    ax.set_ylabel('Cumulative offset (relative to cardWidth)')
    ax.set_title('Left Cards Geometric Series Convergence\noffset(d) = basePeek × (1 - decay^d) / (1 - decay)')
    ax.legend(loc='lower right')
    ax.grid(True, alpha=0.3)
    ax.set_xlim(-0.2, 6)
    ax.set_ylim(0, 0.35)

    # Add formula box
    formula_text = f'basePeek = {basePeek}\ndecay = {decay}\nlimit = {limit:.4f}'
    props = dict(boxstyle='round,pad=0.5', facecolor='#0f3460', edgecolor=CYAN, alpha=0.9)
    ax.text(0.02, 0.98, formula_text, transform=ax.transAxes, fontsize=10,
            verticalalignment='top', bbox=props, family='monospace')

    save_chart(fig, '01_geometric_series.png')


def chart_02_depth_scale():
    """Chart 2: Asymmetric depth scale function."""
    fig, ax = plt.subplots(figsize=(10, 6))

    relPos = np.linspace(-4, 3, 300)

    def depth_scale(r):
        if r >= 0:
            focusedScale = 0.98
            return min(focusedScale + (1 - focusedScale) * r, 1.0)
        else:
            minScale = 0.96
            focusedScale = 0.98
            decay = 0.50
            return minScale + (focusedScale - minScale) * (decay ** (-r))

    scale = np.array([depth_scale(r) for r in relPos])

    ax.plot(relPos, scale, color=CYAN, linewidth=2.5, label='depthScale(relPos)')
    ax.axvline(x=0, color=YELLOW, linestyle=':', linewidth=1.5, alpha=0.7, label='Focus position')
    ax.axhline(y=0.98, color=MAGENTA, linestyle='--', linewidth=1.5, alpha=0.7, label='Focus scale 0.98')
    ax.axhline(y=1.0, color=GREEN, linestyle='--', linewidth=1.5, alpha=0.7, label='Max scale 1.0')
    ax.axhline(y=0.96, color=ORANGE, linestyle='--', linewidth=1.5, alpha=0.7, label='Min scale 0.96')

    # Fill regions
    ax.fill_between(relPos[relPos < 0], 0.94, scale[relPos < 0],
                   alpha=0.2, color=MAGENTA, label='Left region')
    ax.fill_between(relPos[relPos >= 0], 0.94, scale[relPos >= 0],
                   alpha=0.2, color=CYAN, label='Right region')

    ax.set_xlabel('Relative position relPos')
    ax.set_ylabel('Scale ratio')
    ax.set_title('Asymmetric Depth Scale Function\nLeft: exponential decay | Right: linear climb')
    ax.legend(loc='lower right', ncol=2)
    ax.grid(True, alpha=0.3)
    ax.set_xlim(-4, 3)
    ax.set_ylim(0.94, 1.02)

    save_chart(fig, '02_depth_scale.png')


def chart_03_title_alpha_blur():
    """Chart 3: Title alpha and blur radius."""
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(12, 5))

    relPos = np.linspace(-3, 3, 300)

    # Title alpha: clamp(min(1 + relPos, 2 - relPos), 0, 1)
    titleAlpha = np.clip(np.minimum(1 + relPos, 2 - relPos), 0, 1)

    # Blur radius: min(1 - titleAlpha, 0.5) * 20
    blurRadius = np.minimum(1 - titleAlpha, 0.5) * 20

    # Plot alpha
    ax1.plot(relPos, titleAlpha, color=CYAN, linewidth=2.5, label='titleAlpha')
    ax1.fill_between(relPos, 0, titleAlpha, alpha=0.3, color=CYAN)
    ax1.axvline(x=0, color=YELLOW, linestyle=':', linewidth=1.5, alpha=0.7)
    ax1.axvline(x=1, color=YELLOW, linestyle=':', linewidth=1.5, alpha=0.7)
    ax1.set_xlabel('Relative position relPos')
    ax1.set_ylabel('Alpha')
    ax1.set_title('Title Alpha\nclamp(min(1+relPos, 2-relPos), 0, 1)')
    ax1.grid(True, alpha=0.3)
    ax1.set_xlim(-3, 3)
    ax1.set_ylim(-0.1, 1.1)

    # Plot blur
    ax2.plot(relPos, blurRadius, color=MAGENTA, linewidth=2.5, label='blurRadius')
    ax2.fill_between(relPos, 0, blurRadius, alpha=0.3, color=MAGENTA)
    ax2.axhline(y=10, color=ORANGE, linestyle='--', linewidth=1.5, alpha=0.7, label='Max blur 10dp')
    ax2.set_xlabel('Relative position relPos')
    ax2.set_ylabel('Blur radius (dp)')
    ax2.set_title('Title Blur Radius\nmin(1-alpha, 0.5) × 20dp')
    ax2.legend(loc='upper right')
    ax2.grid(True, alpha=0.3)
    ax2.set_xlim(-3, 3)
    ax2.set_ylim(-0.5, 12)

    save_chart(fig, '03_title_alpha_blur.png')


def chart_04_overscroll_weights():
    """Chart 4: Updated overscroll weights with new formulas."""
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 6))

    d = np.linspace(0, 5, 200)

    # Left edge overscroll (overscroll < 0): 0.72 + 0.55 * (d / (d + 0.6))
    left_weight = 0.72 + 0.55 * (d / (d + 0.6))

    # Right edge overscroll (overscroll > 0): 0.65 / (d + 1)
    right_weight = 0.65 / (d + 1)

    # Left edge plot
    ax1.plot(d, left_weight, color=CYAN, linewidth=2.5, label='weight = 0.72 + 0.55 × d/(d+0.6)')

    # Mark integer points
    for i in range(5):
        w = 0.72 + 0.55 * (i / (i + 0.6))
        ax1.plot(i, w, 'o', color=YELLOW, markersize=10, zorder=5)
        ax1.annotate(f'd={i}: {w:.2f}', (i, w), textcoords="offset points",
                    xytext=(8, 8), fontsize=9, color=YELLOW)

    ax1.set_xlabel('Depth d = relPos (distance from edge card)')
    ax1.set_ylabel('Displacement weight')
    ax1.set_title('Left Edge Overscroll Weight (Fan Out)\nEdge card moves less, far cards move more')
    ax1.legend(loc='lower right')
    ax1.grid(True, alpha=0.3)
    ax1.set_xlim(-0.2, 5)
    ax1.set_ylim(0, 1.5)

    # Add visual explanation
    props = dict(boxstyle='round,pad=0.5', facecolor='#0f3460', edgecolor=CYAN, alpha=0.9)
    ax1.text(0.98, 0.02, 'Edge card weight=0.72\nFar cards weight->1.27',
            transform=ax1.transAxes, fontsize=10, ha='right', va='bottom',
            bbox=props, family='monospace')

    # Right edge plot
    ax2.plot(d, right_weight, color=MAGENTA, linewidth=2.5, label='weight = 0.65 / (d+1)')

    # Mark integer points
    for i in range(5):
        w = 0.65 / (i + 1)
        ax2.plot(i, w, 'o', color=YELLOW, markersize=10, zorder=5)
        ax2.annotate(f'd={i}: {w:.2f}', (i, w), textcoords="offset points",
                    xytext=(8, 8), fontsize=9, color=YELLOW)

    ax2.set_xlabel('Depth d = -relPos (distance from edge card)')
    ax2.set_ylabel('Displacement weight')
    ax2.set_title('Right Edge Overscroll Weight (Stretch)\nRightmost card moves most, left cards move less')
    ax2.legend(loc='upper right')
    ax2.grid(True, alpha=0.3)
    ax2.set_xlim(-0.2, 5)
    ax2.set_ylim(0, 0.8)

    # Add visual explanation
    props = dict(boxstyle='round,pad=0.5', facecolor='#0f3460', edgecolor=MAGENTA, alpha=0.9)
    ax2.text(0.98, 0.98, 'Rightmost weight=0.65\nFar cards weight->0',
            transform=ax2.transAxes, fontsize=10, ha='right', va='top',
            bbox=props, family='monospace')

    save_chart(fig, '04_overscroll_weights.png')


def chart_05_spring_damping():
    """Chart 5: Spring damping modes comparison."""
    fig, ax = plt.subplots(figsize=(10, 6))

    t = np.linspace(0, 2, 500)

    # Different damping ratios
    # Critical damping (ζ = 1.0)
    omega = np.sqrt(80)  # stiffness = 80
    critical = 1 - (1 + omega * t) * np.exp(-omega * t)

    # Under-damped (ζ = 0.7)
    zeta = 0.7
    omega_d = omega * np.sqrt(1 - zeta**2)
    under = 1 - np.exp(-zeta * omega * t) * (np.cos(omega_d * t) +
            (zeta / np.sqrt(1 - zeta**2)) * np.sin(omega_d * t))

    # Over-damped (ζ = 1.5)
    zeta = 1.5
    r1 = -omega * (zeta + np.sqrt(zeta**2 - 1))
    r2 = -omega * (zeta - np.sqrt(zeta**2 - 1))
    over = 1 - (r2 * np.exp(r1 * t) - r1 * np.exp(r2 * t)) / (r2 - r1)

    ax.plot(t, critical, color=CYAN, linewidth=2.5, label='Critical damping z=1.0 (current)')
    ax.plot(t, under, color=MAGENTA, linewidth=2, linestyle='--', label='Under-damped z=0.7 (oscillates)')
    ax.plot(t, over, color=ORANGE, linewidth=2, linestyle=':', label='Over-damped z=1.5 (too slow)')

    ax.axhline(y=1, color=GREEN, linestyle='-', linewidth=1, alpha=0.5, label='Target position')

    ax.set_xlabel('Time (seconds)')
    ax.set_ylabel('Position (normalized)')
    ax.set_title('Spring Damping Modes Comparison\nSpring(dampingRatio=1.0, stiffness=80)')
    ax.legend(loc='lower right')
    ax.grid(True, alpha=0.3)
    ax.set_xlim(0, 2)
    ax.set_ylim(-0.1, 1.3)

    # Highlight optimal region
    ax.fill_between(t, 0.95, 1.05, alpha=0.2, color=GREEN, label='+/- 5% range')

    save_chart(fig, '05_spring_damping.png')


def chart_06_bezier_easing():
    """Chart 6: Cubic bezier easing curve."""
    fig, ax = plt.subplots(figsize=(10, 6))

    # Control points for CubicBezierEasing(0.17, 0.84, 0.44, 1.0)
    p0 = (0, 0)
    p1 = (0.17, 0.84)
    p2 = (0.44, 1.0)
    p3 = (1, 1)

    t = np.linspace(0, 1, 200)

    # Cubic bezier formula
    x = (1-t)**3 * p0[0] + 3*(1-t)**2 * t * p1[0] + 3*(1-t) * t**2 * p2[0] + t**3 * p3[0]
    y = (1-t)**3 * p0[1] + 3*(1-t)**2 * t * p1[1] + 3*(1-t) * t**2 * p2[1] + t**3 * p3[1]

    ax.plot(x, y, color=CYAN, linewidth=3, label='iOS easing curve')
    ax.plot([0, 1], [0, 1], color=YELLOW, linewidth=1.5, linestyle='--', alpha=0.5, label='Linear')

    # Plot control points
    ax.plot([p0[0], p1[0]], [p0[1], p1[1]], 'o--', color=MAGENTA, markersize=8, linewidth=1.5)
    ax.plot([p3[0], p2[0]], [p3[1], p2[1]], 'o--', color=MAGENTA, markersize=8, linewidth=1.5)

    ax.annotate('P1 (0.17, 0.84)', p1, textcoords="offset points",
               xytext=(-60, 10), fontsize=9, color=MAGENTA)
    ax.annotate('P2 (0.44, 1.0)', p2, textcoords="offset points",
               xytext=(10, -20), fontsize=9, color=MAGENTA)

    ax.set_xlabel('Input progress t')
    ax.set_ylabel('Output progress p')
    ax.set_title('Bezier Easing Curve\nCubicBezierEasing(0.17, 0.84, 0.44, 1.0)')
    ax.legend(loc='lower right')
    ax.grid(True, alpha=0.3)
    ax.set_xlim(-0.05, 1.05)
    ax.set_ylim(-0.05, 1.1)
    ax.set_aspect('equal')

    # Add characteristics text
    props = dict(boxstyle='round,pad=0.5', facecolor='#0f3460', edgecolor=CYAN, alpha=0.9)
    ax.text(0.02, 0.98, 'Features:\n- Gentle start\n- Fast peak\n- Long tail\n- No overshoot',
            transform=ax.transAxes, fontsize=10, verticalalignment='top',
            bbox=props, family='sans-serif')

    save_chart(fig, '06_bezier_easing.png')


def chart_07_damped_overscroll():
    """Chart 7: New damped overscroll function."""
    fig, ax = plt.subplots(figsize=(10, 6))

    overscroll = np.linspace(0, 3, 200)

    # New formula: dampedOverscroll = absOver / (1 + absOver * 0.8)
    damped = overscroll / (1 + overscroll * 0.8)

    # Old formula for comparison (linear with friction)
    old_linear = overscroll * 0.21  # base 0.70 * edge 0.30

    ax.plot(overscroll, damped, color=CYAN, linewidth=2.5,
            label='New: x / (1 + 0.8x)')
    ax.plot(overscroll, old_linear, color=ORANGE, linewidth=2, linestyle='--',
            label='Old: x * 0.21 (linear)')
    ax.plot(overscroll, overscroll, color=YELLOW, linewidth=1.5, linestyle=':',
            alpha=0.5, label='No damping (1:1)')

    # Mark key points
    for x in [0.5, 1.0, 2.0]:
        y = x / (1 + x * 0.8)
        ax.plot(x, y, 'o', color=CYAN, markersize=10, zorder=5)
        ax.annotate(f'({x}, {y:.2f})', (x, y), textcoords="offset points",
                   xytext=(10, 5), fontsize=9, color=CYAN)

    ax.set_xlabel('Raw overscroll amount')
    ax.set_ylabel('Damped displacement')
    ax.set_title('Smooth Damping Function\nResponsive at start, harder to pull further (no jumps)')
    ax.legend(loc='lower right')
    ax.grid(True, alpha=0.3)
    ax.set_xlim(-0.1, 3)
    ax.set_ylim(-0.1, 1.5)

    # Add formula box
    props = dict(boxstyle='round,pad=0.5', facecolor='#0f3460', edgecolor=CYAN, alpha=0.9)
    ax.text(0.02, 0.98, 'dampedOverscroll = |overscroll| / (1 + |overscroll| * 0.8)\n\n'
            '- Derivative=1 at x=0 (responsive)\n'
            '- Asymptote y=1.25 (finite limit)\n'
            '- No sqrt discontinuity',
            transform=ax.transAxes, fontsize=10, verticalalignment='top',
            bbox=props, family='monospace')

    save_chart(fig, '07_damped_overscroll.png')


def chart_08_z_sink_scale():
    """Chart 8: Z-axis sink scale for right edge overscroll."""
    fig, ax = plt.subplots(figsize=(10, 6))

    d = np.linspace(0, 4, 200)
    overscroll_values = [0.5, 1.0, 1.5, 2.0]
    colors = [CYAN, GREEN, YELLOW, MAGENTA]

    for overscroll, color in zip(overscroll_values, colors):
        # sinkWeight = 0.3 + 0.7 * (d / (d + 0.8))
        sinkWeight = 0.3 + 0.7 * (d / (d + 0.8))
        # dampedOverscroll = overscroll / (1 + overscroll * 0.8)
        dampedOver = overscroll / (1 + overscroll * 0.8)
        # scale = 1 - dampedOverscroll * 0.15 * sinkWeight
        scale = 1 - dampedOver * 0.15 * sinkWeight

        ax.plot(d, scale, color=color, linewidth=2.5,
                label=f'overscroll = {overscroll}')

    # Mark key points for overscroll=1.0
    dampedOver = 1.0 / (1 + 1.0 * 0.8)
    for i in [0, 1, 2, 3]:
        sinkWeight = 0.3 + 0.7 * (i / (i + 0.8))
        scale = 1 - dampedOver * 0.15 * sinkWeight
        ax.plot(i, scale, 'o', color=GREEN, markersize=8, zorder=5)

    ax.set_xlabel('Depth d = -relPos (distance from rightmost card)')
    ax.set_ylabel('Scale ratio')
    ax.set_title('Right Edge Overscroll Z-axis Sink Effect\nRightmost card shrinks little, left cards shrink more')
    ax.legend(loc='lower left')
    ax.grid(True, alpha=0.3)
    ax.set_xlim(-0.2, 4)
    ax.set_ylim(0.8, 1.02)

    # Add formula box
    props = dict(boxstyle='round,pad=0.5', facecolor='#0f3460', edgecolor=CYAN, alpha=0.9)
    ax.text(0.98, 0.02, 'sinkWeight = 0.3 + 0.7 * d/(d+0.8)\n'
            'scale = 1 - damped * 0.15 * sinkWeight\n\n'
            'd=0: sinkWeight=0.3 (shrink less)\n'
            'd->inf: sinkWeight->1.0 (shrink more)',
            transform=ax.transAxes, fontsize=10, ha='right', va='bottom',
            bbox=props, family='monospace')

    save_chart(fig, '08_z_sink_scale.png')


def chart_09_progressive_friction():
    """Chart 9: Progressive drag friction."""
    fig, ax = plt.subplots(figsize=(10, 6))

    overscroll = np.linspace(0, 3, 200)

    # New formula: 0.6 / (1 + overscroll * 0.5)
    friction = 0.6 / (1 + overscroll * 0.5)
    total_friction = 0.70 * friction  # base friction * edge friction

    # Old formula (constant)
    old_friction = np.ones_like(overscroll) * 0.21

    ax.plot(overscroll, friction, color=CYAN, linewidth=2.5,
            label='Edge friction 0.6/(1+0.5x)')
    ax.plot(overscroll, total_friction, color=MAGENTA, linewidth=2.5,
            label='Total friction (* 0.70)')
    ax.axhline(y=0.21, color=ORANGE, linestyle='--', linewidth=2,
               label='Old formula (fixed 0.21)')

    # Mark key points
    for x in [0, 1, 2]:
        f = 0.6 / (1 + x * 0.5)
        t = 0.70 * f
        ax.plot(x, f, 'o', color=CYAN, markersize=10, zorder=5)
        ax.plot(x, t, 'o', color=MAGENTA, markersize=10, zorder=5)
        ax.annotate(f'{f:.2f}', (x, f), textcoords="offset points",
                   xytext=(10, 5), fontsize=9, color=CYAN)
        ax.annotate(f'{t:.2f}', (x, t), textcoords="offset points",
                   xytext=(10, -15), fontsize=9, color=MAGENTA)

    ax.set_xlabel('Current overscroll amount')
    ax.set_ylabel('Friction coefficient')
    ax.set_title('Progressive Drag Friction\nResponsive at start, resistance increases as you pull')
    ax.legend(loc='upper right')
    ax.grid(True, alpha=0.3)
    ax.set_xlim(-0.1, 3)
    ax.set_ylim(0, 0.7)

    # Add comparison box
    props = dict(boxstyle='round,pad=0.5', facecolor='#0f3460', edgecolor=CYAN, alpha=0.9)
    ax.text(0.02, 0.02, 'Comparison:\n'
            '- Old: fixed 0.21 (hard to pull)\n'
            '- New: starts 0.42 -> decreases\n'
            '  Initial response 2x better',
            transform=ax.transAxes, fontsize=10, va='bottom',
            bbox=props, family='monospace')

    save_chart(fig, '09_progressive_friction.png')


def main():
    """Generate all charts."""
    print("Generating charts...")

    # Ensure charts directory exists
    CHARTS_DIR.mkdir(parents=True, exist_ok=True)

    chart_01_geometric_series()
    chart_02_depth_scale()
    chart_03_title_alpha_blur()
    chart_04_overscroll_weights()
    chart_05_spring_damping()
    chart_06_bezier_easing()
    chart_07_damped_overscroll()
    chart_08_z_sink_scale()
    chart_09_progressive_friction()

    print("\nAll charts generated successfully!")


if __name__ == '__main__':
    main()
