export default { /** stage-tpg */
  apiUrl: 'https://stage-tpg.foxcommerce.com',
  storefronts: [
    {
      name: 'TPG',
      url: 'https://stage-tpg.foxcommerce.com/',
      categories: ['APPETIZERS', 'ENTRÉES', 'SIDES', 'BEST-SELLERS', 'GIFT-CARDS'],
      aboutPagePath: 'about',
    },
  ],
  stripeKey: 'pk_test_JvTXpI3DrkV6QwdcmZarmlfk',
  networkErrorRetries: 4,
  testGiftCardFlow: true,
};

// export default { /** stage */
//   apiUrl: 'https://stage.foxcommerce.com',
//   storefronts: [
//     {
//       name: 'TPG',
//       url: 'https://stage.foxcommerce.com/perfect-gourmet',
//       categories: ['APPETIZERS', 'ENTRÉES', 'SIDES', 'BEST-SELLERS', 'GIFT-CARDS'],
//       aboutPagePath: 'about',
//     },
//     {
//       name: 'TD',
//       url: 'https://stage.foxcommerce.com/top-drawer',
//       categories: ['classic', 'modern', 'custom'],
//     },
//   ],
//   stripeKey: 'pk_test_JvTXpI3DrkV6QwdcmZarmlfk',
//   networkErrorRetries: 4,
//   testGiftCardFlow: false,
// };
