
import { get } from 'sprout-data';
import makePagination from '../pagination';

const {
  reducer,
  actions: {
    fetch,
    setFetchParams,
    actionAddEntity
    }
  } = makePagination('/gift-cards', 'GIFT_CARDS');

export function createGiftCard() {
  return (dispatch, getState) => {
    const addingData = get(getState(), ['giftCards', 'adding']);

      // @TODO: select correct data from state for gift card creation
        // please don't ask me for get rid of this todo now, this task is not part of reduxing story.
          Api.post('/gift-cards', giftCardsNew)
      .then(json => dispatch(updateGiftCards([json])))
      .catch(err => dispatch(failGiftCards(err)));
  };
}


export default reducer;

export {
  fetch,
  setFetchParams
};
