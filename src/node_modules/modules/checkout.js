
import {createAction, createReducer} from 'redux-act';

export type EditStage = 'shipping' | 'delivery' | 'billing';

export type CheckoutState = {
  editStage: EditStage;
};

export const setEditStage = createAction('CHECKOUT_SET_EDIT_STAGE');

const initialState: CheckoutState = {
  editStage: 'shipping',
};

const reducer = createReducer({
  [setEditStage]: (state, editStage) => {
    return {
      ...state,
      editStage,
    };
  },
}, initialState);

export default reducer;
