import { useState } from 'react';
import { card, muted } from '../ui';

/*
 * Small inline-SVG charts for the dashboard. Colours are validated for colour-vision
 * deficiency and contrast (dataviz six-checks): #059669 / #f59e0b / #dc2626 on white.
 * Identity is never colour-alone: every slice is named in the legend with its count,
 * and every mark answers hover with a tooltip.
 */

interface Tooltip {
  x: number;
  y: number;
  text: string;
}

function useTooltip() {
  const [tip, setTip] = useState<Tooltip | null>(null);
  const show = (event: React.MouseEvent, text: string) => {
    const host = (event.currentTarget as SVGElement).closest('[data-chart]');
    if (!host) return;
    const box = host.getBoundingClientRect();
    setTip({ x: event.clientX - box.left, y: event.clientY - box.top, text });
  };
  const hide = () => setTip(null);
  return { tip, show, hide };
}

function TooltipBox({ tip }: { tip: Tooltip | null }) {
  if (!tip) return null;
  return (
    <div
      className="pointer-events-none absolute z-10 -translate-x-1/2 -translate-y-full rounded-lg bg-slate-900 px-2.5 py-1.5 text-xs font-medium whitespace-nowrap text-white shadow-lg dark:bg-slate-700"
      style={{ left: tip.x, top: tip.y - 8 }}
    >
      {tip.text}
    </div>
  );
}

// ---- donut: composition of a small set of states ------------------------------------------------

export interface Slice {
  label: string;
  value: number;
  color: string;
}

/** One donut arc per non-zero slice, with a 2-degree gap between neighbours. */
export function StatusDonut({ title, slices, centerLabel }: { title: string; slices: Slice[]; centerLabel: string }) {
  const { tip, show, hide } = useTooltip();
  const present = slices.filter((slice) => slice.value > 0);
  const total = present.reduce((sum, slice) => sum + slice.value, 0);

  const radius = 42;
  const stroke = 14;
  const arcs = buildArcs(present, total, radius);

  return (
    <div className={`${card} relative`} data-chart="">
      <h3 className="mb-3 font-semibold">{title}</h3>
      {total === 0 ? (
        <p className={muted}>Nothing to show yet.</p>
      ) : (
        <div className="flex flex-wrap items-center gap-5">
          <div className="relative">
            <svg viewBox="-55 -55 110 110" className="h-36 w-36" role="img" aria-label={title}>
              {arcs.map(({ slice, d }) => (
                <path
                  key={slice.label}
                  d={d}
                  fill="none"
                  stroke={slice.color}
                  strokeWidth={stroke}
                  strokeLinecap="round"
                  className="transition-opacity hover:opacity-80"
                  onMouseMove={(event) => show(event, `${slice.label}: ${slice.value}`)}
                  onMouseLeave={hide}
                />
              ))}
              <text textAnchor="middle" dy="-2" className="fill-slate-900 text-[20px] font-bold dark:fill-slate-100">
                {total}
              </text>
              <text textAnchor="middle" dy="14" className="fill-slate-500 text-[9px] dark:fill-slate-400">
                {centerLabel}
              </text>
            </svg>
          </div>
          <ul className="flex flex-col gap-1.5 text-sm">
            {slices.map((slice) => (
              <li key={slice.label} className="flex items-center gap-2">
                <span className="h-2.5 w-2.5 shrink-0 rounded-full" style={{ background: slice.color }} />
                <span>{slice.label}</span>
                <span className="font-semibold">{slice.value}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
      <TooltipBox tip={tip} />
    </div>
  );
}

function point(degrees: number, radius: number): string {
  const rad = (degrees * Math.PI) / 180;
  return `${(Math.cos(rad) * radius).toFixed(2)} ${(Math.sin(rad) * radius).toFixed(2)}`;
}

/** One SVG arc per slice, starting at 12 o'clock, with a 2-degree gap between neighbours. */
function buildArcs(present: Slice[], total: number, radius: number): { slice: Slice; d: string }[] {
  const gapDegrees = present.length > 1 ? 2 : 0;
  let angle = -90;
  return present.map((slice) => {
    const sweep = (slice.value / total) * 360 - gapDegrees;
    const from = angle + gapDegrees / 2;
    const to = from + Math.max(sweep, 0.5);
    angle += (slice.value / total) * 360;
    const large = to - from > 180 ? 1 : 0;
    return { slice, d: `M ${point(from, radius)} A ${radius} ${radius} 0 ${large} 1 ${point(to, radius)}` };
  });
}

// ---- bars: magnitude per day over the coming week ----------------------------------------------

export interface Day {
  label: string;
  value: number;
}

/** One teal series, rounded data-ends, the busiest day direct-labelled, the rest on hover. */
export function WeekBars({ title, days, unit }: { title: string; days: Day[]; unit: string }) {
  const { tip, show, hide } = useTooltip();
  const max = Math.max(...days.map((day) => day.value), 0);
  const width = 240;
  const height = 110;
  const plotHeight = 84;
  const step = width / days.length;
  const barWidth = Math.min(22, step - 8);

  return (
    <div className={`${card} relative`} data-chart="">
      <h3 className="mb-3 font-semibold">{title}</h3>
      {max === 0 ? (
        <p className={muted}>No {unit} in the next 7 days.</p>
      ) : (
        <svg viewBox={`0 0 ${width} ${height}`} className="w-full max-w-xs" role="img" aria-label={title}>
          <line x1="0" y1={plotHeight} x2={width} y2={plotHeight} className="stroke-slate-200 dark:stroke-slate-700" />
          {days.map((day, index) => {
            const barHeight = max === 0 ? 0 : (day.value / max) * (plotHeight - 14);
            const x = index * step + (step - barWidth) / 2;
            const y = plotHeight - barHeight;
            return (
              <g key={day.label}>
                {day.value > 0 && (
                  <rect
                    x={x}
                    y={y}
                    width={barWidth}
                    height={Math.max(barHeight, 2)}
                    rx="4"
                    fill="#0d9488"
                    className="transition-opacity hover:opacity-80"
                    onMouseMove={(event) => show(event, `${day.label}: ${day.value} ${unit}`)}
                    onMouseLeave={hide}
                  />
                )}
                {day.value === max && (
                  <text x={x + barWidth / 2} y={y - 4} textAnchor="middle" className="fill-slate-600 text-[9px] font-semibold dark:fill-slate-300">
                    {day.value}
                  </text>
                )}
                <text x={index * step + step / 2} y={height - 2} textAnchor="middle" className="fill-slate-500 text-[9px] dark:fill-slate-400">
                  {day.label}
                </text>
              </g>
            );
          })}
        </svg>
      )}
      <TooltipBox tip={tip} />
    </div>
  );
}
