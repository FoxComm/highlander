const TPG_CATEGORIES = ['APPETIZERS', 'ENTRÉES', 'SIDES', 'BEST-SELLERS', 'GIFT-CARDS'];
const TD_CATEGORIES = ['classic', 'modern', 'custom'];

export default {
  'STAGE': {
    apiUrl: 'https://stage.foxcommerce.com',
    storefronts: [
      {
        name: 'TPG',
        url: 'https://stage.foxcommerce.com/perfect-gourmet',
        categories: TPG_CATEGORIES,
        aboutPagePath: 'about',
      },
      {
        name: 'TD',
        url: 'https://stage.foxcommerce.com/top-drawer',
        categories: TD_CATEGORIES,
      },
    ],
    stripeKey: 'pk_test_JvTXpI3DrkV6QwdcmZarmlfk',
    networkErrorRetries: 4,
    testGiftCardFlow: false,
    fullApiSequenceLogging: false,
  },
  'STAGE-TPG': {
    apiUrl: 'https://stage-tpg.foxcommerce.com',
    storefronts: [
      {
        name: 'TPG',
        url: 'https://stage-tpg.foxcommerce.com/',
        categories: TPG_CATEGORIES,
        aboutPagePath: 'about',
      },
    ],
    stripeKey: 'pk_test_JvTXpI3DrkV6QwdcmZarmlfk',
    networkErrorRetries: 4,
    testGiftCardFlow: true,
    fullApiSequenceLogging: false,
  },
  'STAGE-TD': {
    apiUrl: 'https://td-prod.foxcommerce.com',
    storefronts: [
      {
        name: 'TD',
        url: 'https://td-prod.foxcommerce.com/',
        categories: TD_CATEGORIES,
      },
    ],
    stripeKey: 'pk_test_JvTXpI3DrkV6QwdcmZarmlfk',
    networkErrorRetries: 4,
    testGiftCardFlow: false,
    fullApiSequenceLogging: false,
  },
  'TEST': {
    apiUrl: 'https://test.foxcommerce.com',
    storefronts: [
      {
        name: 'TPG',
        url: 'https://test.foxcommerce.com/perfect-gourmet',
        categories: TPG_CATEGORIES,
        aboutPagePath: 'about',
      },
      {
        name: 'TD',
        url: 'https://test.foxcommerce.com/top-drawer',
        categories: TD_CATEGORIES,
      },
    ],
    stripeKey: 'pk_test_JvTXpI3DrkV6QwdcmZarmlfk',
    networkErrorRetries: 4,
    testGiftCardFlow: false,
    fullApiSequenceLogging: false,
  },
  'FEATURE-BRANCH-APPLE': {
    apiUrl: 'https://feature-branch-apple.foxcommerce.com',
    storefronts: [
      {
        name: 'TPG',
        url: 'https://feature-branch-apple.foxcommerce.com/perfect-gourmet',
        categories: TPG_CATEGORIES,
        aboutPagePath: 'about',
      },
      {
        name: 'TD',
        url: 'https://feature-branch-apple.foxcommerce.com/top-drawer',
        categories: TD_CATEGORIES,
      },
    ],
    stripeKey: 'pk_test_JvTXpI3DrkV6QwdcmZarmlfk',
    networkErrorRetries: 4,
    testGiftCardFlow: false,
    fullApiSequenceLogging: false,
  },
  'APPLIANCE-7': {
    apiUrl: 'https://appliance-10-240-0-7.foxcommerce.com',
    storefronts: [
      {
        name: 'TPG',
        url: 'https://appliance-10-240-0-7.foxcommerce.com/perfect-gourmet',
        categories: TPG_CATEGORIES,
        aboutPagePath: 'about',
      },
      {
        name: 'TD',
        url: 'https://appliance-10-240-0-7.foxcommerce.com/top-drawer',
        categories: TD_CATEGORIES,
      },
    ],
    stripeKey: 'pk_test_JvTXpI3DrkV6QwdcmZarmlfk',
    networkErrorRetries: 4,
    testGiftCardFlow: false,
    fullApiSequenceLogging: false,
  },
}[process.env['BVT_ENV']];
