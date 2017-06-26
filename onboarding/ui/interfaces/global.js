declare module CSSModule {
  declare var exports: { [key: string]: string };
}

declare class SEvent<TargetType: HTMLElement> {
  bubbles: boolean;
  cancelable: boolean;
  currentTarget: TargetType;
  defaultPrevented: boolean;
  eventPhase: number;
  isDefaultPrevented(): boolean;
  isPropagationStopped(): boolean;
  isTrusted: boolean;
  nativeEvent: Event;
  preventDefault(): void;
  stopPropagation(): void;
  target: TargetType;
  timeStamp: number;
  type: string;
}

type Thenable = {
 then: (onSuccess: Function, onFailure: Function) => any;
}

declare function makeXhr(url: string): XMLHttpRequest & Thenable;

