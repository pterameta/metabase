import { useRef, useCallback } from "react";

import { AsyncFn } from "metabase-types/types";
type AbortFn = () => void;
export type AsyncFnWithOptionalAbort = (
  ...args: any[]
) => Promise<any> | [Promise<any>, AbortFn];

export function useMostRecentCall(fn: AsyncFnWithOptionalAbort): AsyncFn {
  const ref = useRef<[] | [Promise<any>] | [Promise<any>, AbortFn]>([]);

  return useCallback(
    async (...args: any[]) => {
      const promiseOrPromiseWithAbort = fn(...args);
      if (typeof ref.current[1] === "function") {
        ref.current[1]();
      }

      ref.current = Array.isArray(promiseOrPromiseWithAbort)
        ? promiseOrPromiseWithAbort
        : [promiseOrPromiseWithAbort];

      const promise = ref.current[0];

      return new Promise((resolve, reject) => {
        return promise
          .then((res: any) => {
            if (ref.current[0] === promise) {
              resolve(res);
            }
          })
          .catch((err: Error) => {
            if (ref.current[0] === promise) {
              reject(err);
            }
          });
      });
    },
    [fn],
  );
}
