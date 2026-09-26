import { useCallback, useEffect, useState } from 'react';
import { describeError } from './api';
import type { Report } from './types';

interface Loaded<T> {
  data: T | null;
  failed: string | null;
  reload: () => void;
}

/** Loads data when the deps change; reload() fetches again. Errors go to report() as messages. */
export function useLoad<T>(loader: () => Promise<T>, deps: readonly unknown[], report?: Report): Loaded<T> {
  const [data, setData] = useState<T | null>(null);
  const [failed, setFailed] = useState<string | null>(null);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setFailed(null);
    loader()
      .then((result) => {
        if (!cancelled) setData(result);
      })
      .catch((error: unknown) => {
        if (cancelled) return;
        const text = describeError(error);
        setData(null);
        setFailed(text);
        report?.({ kind: 'error', text });
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick]);

  const reload = useCallback(() => setTick((value) => value + 1), []);
  return { data, failed, reload };
}

/** Wraps a user action: runs it, shows success or the API's error, then refreshes. */
export function useAction(report: Report, reload?: () => void) {
  return useCallback(
    async (action: () => Promise<unknown>, successText?: string): Promise<boolean> => {
      try {
        await action();
        if (successText) report({ kind: 'ok', text: successText });
        reload?.();
        return true;
      } catch (error) {
        report({ kind: 'error', text: describeError(error) });
        return false;
      }
    },
    [report, reload],
  );
}
