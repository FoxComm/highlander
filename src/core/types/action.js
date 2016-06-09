export type dispatch = (action: any) => void;

export type asyncAction<T> = (dispatch: dispatch) => T;
