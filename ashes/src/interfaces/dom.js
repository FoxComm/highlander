declare type ElementEvent<E> = SyntheticEvent & {
  target: E
};

declare type InputEvent = ElementEvent<HTMLInputElement>;
