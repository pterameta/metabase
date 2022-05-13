import React, { useEffect, useState } from "react";
import { render } from "@testing-library/react";

import {
  useMostRecentCall,
  AsyncFnWithOptionalAbort,
} from "./use-most-recent-call";

function TestComponent({
  trigger,
  asyncFn,
}: {
  trigger: number;
  asyncFn: AsyncFnWithOptionalAbort;
}) {
  const [num, setNum] = useState(0);
  const fn = useMostRecentCall(asyncFn);

  useEffect(() => {
    fn(trigger)
      .then(res => {
        setNum(res);
      })
      .catch(err => {
        setNum(err);
      });
  }, [fn, trigger]);

  return <div>{num}</div>;
}

describe("useMostRecentCall", () => {
  it("should resolve with data once triggered", () => {
    const asyncFn = jest.fn(() => Promise.resolve(1));
    const { findByText } = render(
      <TestComponent asyncFn={asyncFn} trigger={1} />,
    );

    return findByText("1");
  });

  it("should only ever resolve last call's promise", async () => {
    const resolveFnMap: Record<number, () => void> = {};
    const asyncFn = (num: number) =>
      new Promise(resolve => {
        resolveFnMap[num] = resolve.bind(null, num);
      });

    const { rerender, findByText } = render(
      <TestComponent asyncFn={asyncFn} trigger={1} />,
    );

    rerender(<TestComponent asyncFn={asyncFn} trigger={2} />);
    rerender(<TestComponent asyncFn={asyncFn} trigger={3} />);

    await findByText("0");

    // before most recent call resolves
    resolveFnMap[1]();
    // most recent call
    resolveFnMap[3]();
    // after most recent call resolves
    resolveFnMap[2]();

    await findByText("3");
  });

  it("should only reject last call's promise", async () => {
    const rejectFnMap: Record<number, () => void> = {};
    const asyncFn = (num: number) =>
      new Promise((resolve, reject) => {
        rejectFnMap[num] = reject.bind(null, num);
      });

    const { rerender, findByText } = render(
      <TestComponent asyncFn={asyncFn} trigger={1} />,
    );

    rerender(<TestComponent asyncFn={asyncFn} trigger={2} />);
    rerender(<TestComponent asyncFn={asyncFn} trigger={3} />);

    await findByText("0");

    // before most recent call resolves
    rejectFnMap[1]();
    // most recent call
    rejectFnMap[3]();
    // after most recent call resolves
    rejectFnMap[2]();

    await findByText("3");
  });

  it("should support an optional abort function", async () => {
    const resolveFnMap: Record<number, () => void> = {};
    const abortFnMap: Record<number, () => void> = {};
    const asyncFnWithAbort = (num: number): [Promise<number>, () => void] => {
      const abortFn = jest.fn();
      abortFnMap[num] = abortFn;
      return [
        new Promise(resolve => {
          resolveFnMap[num] = resolve.bind(null, num);
        }),
        abortFn,
      ];
    };

    const { rerender, findByText } = render(
      <TestComponent asyncFn={asyncFnWithAbort} trigger={1} />,
    );

    expect(abortFnMap[1]).not.toHaveBeenCalled();

    rerender(<TestComponent asyncFn={asyncFnWithAbort} trigger={2} />);

    expect(abortFnMap[1]).toHaveBeenCalled();

    rerender(<TestComponent asyncFn={asyncFnWithAbort} trigger={3} />);

    expect(abortFnMap[2]).toHaveBeenCalled();

    await findByText("0");

    // most recent call
    resolveFnMap[3]();

    await findByText("3");
  });
});
