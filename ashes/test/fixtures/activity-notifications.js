
import moment from 'moment';
import types from '../../src/components/activity-trail/activities/base/types';

const customer = {
  id: 1,
  name: 'Thomas Angelo',
  email: 'angelo@salieri.it',
  phoneNumber: '+11111111111',
};

const admin = {
  id: 2,
  name: 'Ennio Salieri',
  email: 'don@salieri.it',
  phoneNumber: '+77777777777',
};

const address = {
  'id': 3,
  'region': {
    'id': 4177,
    'countryId': 234,
    'name': 'California'
  },
  'name': 'South',
  'address1': '555 E Lake Union St.',
  'city': 'Los Angeles',
  'zip': '54321',
  'isDefault': false
};

const creditCard = {
  brand: 'Visa',
  lastFour: '3421',
  expMonth: '01',
  expYear: '2018'
};

const giftCard = {
  'id': 6,
  'createdAt': '2016-01-14T19:46:21.272Z',
  'code': 'DDE2CEF877E7C5C5',
  'originId': 1,
  'originType': 'csrAppeasement',
  'state': 'active',
  'currency': 'USD',
  'originalBalance': 5000,
  'availableBalance': 4000,
  'currentBalance': 4000,
  'storeAdmin': {'id': 1, 'email': 'admin@admin.com', 'name': 'Frankly Admin'},
  'message': 'Not implemented yet'
};

const storeCredit = {
  'currency': 'USD',
  'originalBalance': 3000,
  'availableBalance': 2500,
  'currentBalance': 2500,
  'state': 'active',
};

const shippingMethod = {
  id: 1,
  name: 'UPS Ground',
  price: 0
};

const order = {
  referenceNumber: 'BR10001',
  orderStatus: 'cart',
  shippingAddress: {
    'id': 3,
    'region': {
      'id': 4177,
      'countryId': 234,
      'name': 'Washington'
    },
    'name': 'Home',
    'address1': '555 E Lake Union St.',
    'city': 'Seattle',
    'zip': '12345',
    'isDefault': false
  },
  shippingMethod,
};

function getStartDate() {
  return moment('2016-01-20T21:44:23.697Z');
}

let createdAt = getStartDate().toString();
let shiftDays = 1;
let id = 1;

let activities = [
  // customers
  {
    kind: types.CUSTOMER_UPDATED,
    id: id++,
    createdAt,
    data: {
      customerId: 1,
      oldInfo: {
        name: 'William Shockley',
        email: 'William@Shockley.com',
        phoneNumber: '+11111111111'
      },
      newInfo: {
        name: 'Shockley William',
        email: 'William@Shockley.com',
        phoneNumber: '+11111111111'
      }
    }
  },
  {
    kind: types.CUSTOMER_CREATED,
    id: id++,
    createdAt,
    data: {
      customer
    }
  },
  {
    kind: types.CUSTOMER_REGISTERED,
    id: id++,
    createdAt,
    data: {
      customer
    }
  },
  {
    kind: types.CUSTOMER_ACTIVATED,
    id: id++,
    createdAt,
    data: {
      customer
    }
  },
  {
    kind: types.CUSTOMER_BLACKLISTED,
    id: id++,
    createdAt,
    data: {
      customer
    }
  },
  {
    kind: types.CUSTOMER_REMOVED_FROM_BLACKLIST,
    id: id++,
    createdAt,
    data: {
      customer
    }
  },
  {
    kind: types.CUSTOMER_ENABLED,
    id: id++,
    createdAt,
    data: {
      customer
    }
  },
  {
    kind: types.CUSTOMER_DISABLED,
    id: id++,
    createdAt,
    data: {
      customer
    }
  },
];

createdAt = getStartDate().subtract(shiftDays++, 'days').toString();

activities = [...activities,

  // customer addresses

  {
    kind: types.CUSTOMER_ADDRESS_CREATED_BY_ADMIN,
    id: id++,
    createdAt,
    data: {
      customer,
      admin,
      address
    },
    context: {
      userType: 'admin'
    }
  },
  {
    kind: types.CUSTOMER_ADDRESS_CREATED,
    id: id++,
    createdAt,
    data: {
      customer,
      address
    },
    context: {
      userType: 'customer'
    }
  },
  {
    kind: types.CUSTOMER_ADDRESS_UPDATED,
    id: id++,
    createdAt,
    data: {
      customer,
      oldInfo: address,
      newInfo: {
        ...address,
        zip: '23414',
      },
    },
  },
  {
    kind: types.CUSTOMER_ADDRESS_DELETED,
    id: id++,
    createdAt,
    data: {
      customer,
      address,
    },
  },
];

createdAt = getStartDate().subtract(shiftDays++, 'days').toString();

activities = [...activities,

  // order shipping address

  {
    kind: types.CART_SHIPPING_ADDRESS_UPDATED,
    id: id++,
    createdAt,
    data: {
      order,
      address: {
        'id': 3,
        'region': {
          'id': 4177,
          'countryId': 234,
          'name': 'California'
        },
        'name': 'South',
        'address1': '555 E Lake Union St.',
        'city': 'Los Angeles',
        'zip': '54321',
        'isDefault': false
      }
    }
  },
  {
    kind: types.CART_SHIPPING_ADDRESS_ADDED,
    id: id++,
    createdAt,
    data: {
      order,
      address
    }
  },
  {
    kind: types.CART_SHIPPING_ADDRESS_REMOVED,
    id: id++,
    createdAt,
    data: {
      order,
      address
    }
  },
];

createdAt = getStartDate().subtract(shiftDays++, 'days').toString();

const createNote = (body, id, refId, refType) => {
  return {
    body: body,
    createdAt: createdAt,
    id: id,
    referenceId: refId,
    referenceType: refType,
    storeAdminId: 1
  };
};

activities = [...activities,

  // order notes
  {
    kind: types.NOTE_CREATED,
    id: id++,
    createdAt,
    data: {
      entity: {
        referenceNumber: 'BR10001'
      },
      note: createNote('New note for order.', id, 'BR10001', 'order')
    }
  },
  {
    kind: types.NOTE_DELETED,
    id: id++,
    createdAt,
    data: {
      entity: {
        referenceNumber: 'BR10001'
      },
      note: createNote('Lorem ipsum dot color.', id, 'BR10001', 'order')
    }
  },
  {
    kind: types.NOTE_UPDATED,
    id: id++,
    createdAt,
    data: {
      entity: {
        referenceNumber: 'BR10001'
      },
      note: createNote('New one', id, 'BR10001', 'order'),
      oldNote: createNote('Lorem ipsum dot color.', id, 'BR10001', 'order')
    }
  },
];

createdAt = getStartDate().subtract(shiftDays++, 'days').toString();

activities = [...activities,

  // orders
  {
    kind: types.ORDER_STATE_CHANGED,
    id: id++,
    createdAt,
    data: {
      order: {
        referenceNumber: 'BR10004',
        orderStatus: 'fraudHold'
      },
      oldState: 'manualHold',
    }
  },
  {
    kind: types.ORDER_BULK_STATE_CHANGED,
    id: id++,
    createdAt,
    data: {
      newState: 'fraudHold',
      cordRefNums: [
        'BR10004',
        'BR10003',
        'BR10001',
      ],
      oldState: 'manualHold',
    }
  },
  {
    kind: types.CART_CREATED,
    id: id++,
    createdAt,
    data: {
      cart: order,
    }
  }
];


createdAt = getStartDate().subtract(shiftDays++, 'days').toString();

activities = [...activities,
  {
    kind: types.CREDIT_CARD_ADDED,
    id: id++,
    createdAt,
    data: {
      customer,
      creditCard
    }
  },
  {
    kind: types.CREDIT_CARD_REMOVED,
    id: id++,
    createdAt,
    data: {
      customer,
      creditCard
    }
  },
  {
    kind: types.CREDIT_CARD_UPDATED,
    id: id++,
    createdAt,
    data: {
      customer,
      oldInfo: creditCard,
      newInfo: {
        ...creditCard,
        lastFour: '7653'
      }
    }
  },
];

createdAt = getStartDate().subtract(shiftDays++, 'days').toString();

activities = [...activities,
  {
    kind: types.CART_SHIPPING_METHOD_UPDATED,
    id: id++,
    createdAt,
    data: {
      order,
    }
  },
  {
    kind: types.CART_SHIPPING_METHOD_REMOVED,
    id: id++,
    createdAt,
    data: {
      order,
    }
  },
];

createdAt = getStartDate().subtract(shiftDays++, 'days').toString();

activities = [...activities,
  {
    kind: types.CART_PAYMENT_METHOD_ADDED_CREDIT_CARD,
    id: id++,
    createdAt,
    data: {
      order,
      creditCard,
    }
  },
  {
    kind: types.CART_PAYMENT_METHOD_ADDED_GIFT_CARD,
    id: id++,
    createdAt,
    data: {
      order,
      giftCard,
    }
  },
  {
    kind: types.CART_PAYMENT_METHOD_ADDED_STORE_CREDIT,
    id: id++,
    createdAt,
    data: {
      order,
      amount: 420,
    }
  },
  {
    kind: types.CART_PAYMENT_METHOD_DELETED,
    id: id++,
    createdAt,
    data: {
      order,
      pmt: 'giftCard',
    }
  },
  {
    kind: types.CART_PAYMENT_METHOD_DELETED,
    id: id++,
    createdAt,
    data: {
      order,
      pmt: 'creditCard',
    }
  },
  {
    kind: types.CART_PAYMENT_METHOD_DELETED,
    id: id++,
    createdAt,
    data: {
      order,
      pmt: 'storeCredit',
    }
  },
  {
    kind: types.CART_PAYMENT_METHOD_DELETED_GIFT_CARD,
    id: id++,
    createdAt,
    data: {
      order,
      giftCard,
    }
  },
];

createdAt = getStartDate().subtract(shiftDays++, 'days').toString();

activities = [...activities,
  {
    kind: types.GIFT_CARD_CREATED,
    id: id++,
    createdAt,
    data: {
      giftCard,
    }
  },
  {
    kind: types.GIFT_CARD_STATE_CHANGED,
    id: id++,
    createdAt,
    data: {
      giftCard,
    }
  },
  {
    kind: types.GIFT_CARD_CONVERTED_TO_STORE_CREDIT,
    id: id++,
    createdAt,
    data: {
      giftCard,
      storeCredit,
    }
  },
  {
    kind: types.GIFT_CARD_AUTHORIZED_FUNDS,
    id: id++,
    createdAt,
    data: {
      orderRefNum: 'BR10001',
      amount: 21400,
      customer,
      giftCardCodes: [
        '1111333344442222',
        '3333333311115555',
        '7777666600002222',
      ]
    },
    context: {
      userType: 'customer',
    }
  },
  {
    kind: types.GIFT_CARD_CAPTURED_FUNDS,
    id: id++,
    createdAt,
    data: {
      orderRefNum: 'BR10001',
      amount: 124,
      customer,
      giftCardCodes: [
        '1111333344442222',
      ]
    },
    context: {
      userType: 'customer',
    }
  },
];

createdAt = getStartDate().subtract(shiftDays++, 'days').toString();

activities = [...activities,
  {
    kind: types.STORE_CREDIT_CREATED,
    id: id++,
    createdAt,
    data: {
      customer,
      storeCredit,
    }
  },
  {
    kind: types.STORE_CREDIT_STATE_CHANGED,
    id: id++,
    createdAt,
    data: {
      customer,
      storeCredit,
    }
  },
  {
    kind: types.STORE_CREDIT_CONVERTED_TO_GIFT_CARD,
    id: id++,
    createdAt,
    data: {
      customer,
      storeCredit,
      giftCard,
    }
  },
  {
    kind: types.STORE_CREDIT_AUTHORIZED_FUNDS,
    id: id++,
    createdAt,
    data: {
      orderRefNum: 'BR10001',
      amount: 21400,
      customer,
    },
    context: {
      userType: 'customer',
    }
  },
  {
    kind: types.STORE_CREDIT_CAPTURED_FUNDS,
    id: id++,
    createdAt,
    data: {
      orderRefNum: 'BR10001',
      amount: 11400,
      customer,
    },
    context: {
      userType: 'customer',
    }
  },
];

createdAt = getStartDate().subtract(shiftDays++, 'days').toString();

activities = [...activities,
  {
    kind: types.ASSIGNED,
    id: id++,
    createdAt,
    data: {
      entity: order,
      assignees: [
        admin,
      ]
    }
  },
  {
    kind: types.ASSIGNED,
    id: id++,
    createdAt,
    data: {
      entity: order,
      assignees: [
        admin,
        customer,
      ]
    }
  },
  {
    kind: types.UNASSIGNED,
    id: id++,
    createdAt,
    data: {
      entity: order,
      assignee: customer,
    }
  },
  {
    kind: types.BULK_ASSIGNED,
    id: id++,
    createdAt,
    data: {
      assignee: customer,
      entityIds: [
        'BR10001',
        'BR10002',
        'BR10003',
      ]
    }
  },
  {
    kind: types.BULK_UNASSIGNED,
    id: id++,
    createdAt,
    data: {
      assignee: customer,
      entityIds: [
        'BR10001',
        'BR10002',
        'BR10003',
      ]
    }
  },
];

createdAt = getStartDate().subtract(shiftDays++, 'days').toString();

activities = [...activities,
  {
    kind: types.CART_LINE_ITEMS_UPDATED_QUANTITIES,
    id: id++,
    createdAt,
    data: {
      order,
      oldQuantities: {
        'Nike Air Max 2015': 2,
      },
      newQuantities: {
        'Nike Dri-Fit Short-Slevel Women’s Running Shirt': 2,
        'Nike Air Max 2015': 1,
      }
    }
  },
  {
    kind: types.CART_LINE_ITEMS_UPDATED_QUANTITIES_BY_CUSTOMER,
    id: id++,
    createdAt,
    data: {
      order,
      customer,
      oldQuantities: {
        'Nike Air Max 2015': 1,
      },
      newQuantities: {
        'Nike Dri-Fit Short-Slevel Women’s Running Shirt': 2,
      }
    },
    context: {
      userType: 'customer',
    }
  },
  {
    kind: types.CART_LINE_ITEMS_ADDED_GIFT_CARD,
    id: id++,
    createdAt,
    data: {
      order,
      gc: giftCard
    }
  },
  {
    kind: types.CART_LINE_ITEMS_DELETED_GIFT_CARD,
    id: id++,
    createdAt,
    data: {
      order,
      gc: giftCard
    }
  },
  {
    kind: types.CART_LINE_ITEMS_UPDATED_GIFT_CARD,
    id: id++,
    createdAt,
    data: {
      order,
      gc: giftCard
    }
  },
];

export default activities;
