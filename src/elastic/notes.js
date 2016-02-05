
import { post } from '../lib/search';


function buildQuery({entityType, entityId}) {
  switch (entityType) {
    case 'customer':
      return {
        bool: {
          should: [
            {
              nested: {
                path: 'order',
                query: {
                  term: {
                    'order.customerId': {
                      value: entityId
                    }
                  }
                }
              }
            },
            {
              nested: {
                path: 'customer',
                query: {
                  term: {
                    'customer.id': {
                      value: entityId
                    }
                  }
                }
              }
            }
          ],
          minimum_number_should_match: 1
        }
      };
    case 'order':
      return {
        nested: {
          path: 'order',
          query: {
            match: {
              'order.referenceNumber': entityId
            }
          }
        }
      };
    case 'gift-card':
      return {
        nested: {
          path: 'giftCard',
          query: {
            match: {
              'giftCard.code': entityId
            }
          }
        }
      };
      break;
  }
}

export default function searchNotes(entity) {
  const request = {
    query: buildQuery(entity)
  };


  return post('notes_search_view/_search', request);
}
