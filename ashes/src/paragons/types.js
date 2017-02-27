/* @flow */

export type Dictionary<T> = {
  [key: string]: T;
};

// actions
export type Action
  = () => void;
export type Action1<T1>
  = (x1: T1) => void;
export type Action2<T1, T2>
  = (x1: T1, x2: T2) => void;
export type Action3<T1, T2, T3>
  = (x1: T1, x2: T2, x3: T3) => void;
export type Action4<T1, T2, T3, T4>
  = (x1: T1, x2: T2, x3: T3, x4: T4) => void;
export type Action5<T1, T2, T3, T4, T5>
  = (x1: T1, x2: T2, x3: T3, x4: T4, x5: T5) => void;
export type Action6<T1, T2, T3, T4, T5, T6>
  = (x1: T1, x2: T2, x3: T3, x4: T4, x5: T5, x6: T6) => void;
export type Action7<T1, T2, T3, T4, T5, T6, T7>
  = (x1: T1, x2: T2, x3: T3, x4: T4, x5: T5, x6: T6, x7: T7) => void;
export type Action8<T1, T2, T3, T4, T5, T6, T7, T8>
  = (x1: T1, x2: T2, x3: T3, x4: T4, x5: T5, x6: T6, x7: T7, x8: T8) => void;

// functions
export type Func<R>
  = () => R;
export type Func1<T1, R>
  = (x1: T1) => R;
export type Func2<T1, T2, R>
  = (x1: T1, x2: T2) => R;
export type Func3<T1, T2, T3, R>
  = (x1: T1, x2: T2, x3: T3) => R;
export type Func4<T1, T2, T3, T4, R>
  = (x1: T1, x2: T2, x3: T3, x4: T4) => R;
export type Func5<T1, T2, T3, T4, T5, R>
  = (x1: T1, x2: T2, x3: T3, x4: T4, x5: T5) => R;
export type Func6<T1, T2, T3, T4, T5, T6, R>
  = (x1: T1, x2: T2, x3: T3, x4: T4, x5: T5, x6: T6) => R;
export type Func7<T1, T2, T3, T4, T5, T6, T7, R>
  = (x1: T1, x2: T2, x3: T3, x4: T4, x5: T5, x6: T6, x7: T7) => R;
export type Func8<T1, T2, T3, T4, T5, T6, T7, T8, R>
  = (x1: T1, x2: T2, x3: T3, x4: T4, x5: T5, x6: T6, x7: T7, x8: T8) => R;


// async actions
export type AsyncAction
  = Func<Promise<*>>;
export type AsyncAction1<T1>
  = Func1<T1, Promise<*>>;
export type AsyncAction2<T1, T2>
  = Func2<T1, T2, Promise<*>>;
export type AsyncAction3<T1, T2, T3>
  = Func3<T1, T2, T3, Promise<*>>;
export type AsyncAction4<T1, T2, T3, T4>
  = Func4<T1, T2, T3, T4, Promise<*>>;
export type AsyncAction5<T1, T2, T3, T4, T5>
  = Func5<T1, T2, T3, T4, T5, Promise<*>>;
export type AsyncAction6<T1, T2, T3, T4, T5, T6>
  = Func6<T1, T2, T3, T4, T5, T6, Promise<*>>;
export type AsyncAction7<T1, T2, T3, T4, T5, T6, T7>
  = Func7<T1, T2, T3, T4, T5, T6, T7, Promise<*>>;
export type AsyncAction8<T1, T2, T3, T4, T5, T6, T7, T8>
  = Func8<T1, T2, T3, T4, T5, T6, T7, T8, Promise<*>>;
