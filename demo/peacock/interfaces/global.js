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

declare function ga(...args: Array<any>): void;

type EnvVariables = {
  API_URL: string,
  URL_PREFIX: string,
  STRIPE_PUBLISHABLE_KEY: string,
  FIREBIRD_CONTEXT: string,
}

declare var process: {
  env: EnvVariables
}

declare var env: EnvVariables;

