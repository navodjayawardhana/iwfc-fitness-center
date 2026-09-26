import { useCallback, useEffect, useState } from 'react';
import { describeError } from './api.js';

/** Loads data when the deps change; reload() fetches again. Errors go to report() as messages. */
export function useLoad(loader, deps, report) {
  const [data, setData] = useState(null);
  const [failed, setFailed] = useState(null);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setFailed(null);
    loader()
      .then((result) => !cancelled && setData(result))
      .catch((error) => {
        if (cancelled) return;
        setData(null);
        setFailed(describeError(error));
        report?.({ kind: 'error', text: describeError(error) });
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
export function useAction(report, reload) {
  return useCallback(
    async (action, successText) => {
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
