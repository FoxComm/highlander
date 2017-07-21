/* @flow */
/* eslint no-param-reassign: 0 */
/* eslint max-len: 0 */

import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import { categoryNameToUrl } from 'paragons/categories';

const luggageCats = (id: number) => ([
  {
    id: id++,
    name: 'Carry-On Luggage',
    shortDescription: 'Travel light with our collection of carry-on luggage. Perfectly fits in your flight\'s overhead or under the seat and meets all TSA guidelines. Please review Airline Carry-On Guide for size requirements by carrier.'
  },
  {
    id: id++,
    name: 'Checked Luggage',
    shortDescription: 'Elevate your travel experience with our collection of checked luggage. Find everything from rolling to hardside luggage ensuring your belongings arrive in tact.',
  },
  {
    id: id++,
    name: 'Duffels',
    shortDescription: 'Our collection of duffle bags are both sturdy and stylish for any occasion. Choose from a wide selection of gym bags, works bags & many more.',
  },
  {
    id: id++,
    name: 'Garment Bags',
    shortDescription: 'Protect and store your clothing with our high quality garment bags. Shop from products like our TUMI tri-fold carry-on garment bag & durable garment covers.',
  },
]);

const luggageFeatured = (id: number) => ([
  {
    id: id++,
    name: 'What\'s New',
    shortDescription: 'Our travel luggage is unparalleled in reliability, beauty & functionality. Shop our carry-ons, checked luggage, duffel bags & more.',
  },
  {
    id: id++,
    name: 'Best Sellers',
    shortDescription: 'Shop Tumi\'s best selling travel luggage. Check out hot items like our Alpha 2 Carry-Ons and Voyageur duffle bags.',
  },
  {
    id: id++,
    name: 'Personalization Shop',
    shortDescription: 'Stand out from the crowd with complimentary personalization, monograms & unique color accents.',
  },
  {
    id: id++,
    name: 'Spring',
    shortDescription: '',
  },
]);

const luggage = (id: number) => ([
  {
    id: id++,
    name: 'Shop By Category',
    children: luggageCats(id),
    categoryQuery: 'PRODUCTTYPE',
    withViewAll: true,
    skipLink: true,
  },
  {
    id: id++,
    name: 'Featured',
    children: luggageFeatured(id),
    withViewAll: false,
    skipLink: true,
  },
]);

const backpackCats = (id: number) => ([
  {
    id: id++,
    name: 'Travel Backpacks',
    shortDescriptiopn: 'Shop our full collection of lightweight and durable backpacks perfect for travel. Collection includes ballistic backpacks, wheeled backpacks, slings, & more.',
  },
  {
    id: id++,
    name: 'Laptop Backpacks',
    shortDescription: 'Shop our collection of backpacks that fit 12" to 15" laptops. Perfect for both professionals and students.',
  },
  {
    id: id++,
    name: 'Leather Backpacks',
    shortDescription: 'Shop our collection of professional backpacks & sling bags made from premium leathers. Collections include Alpha Bravo, Harrison, Arrivé, & More.',
  },
  {
    id: id++,
    name: 'Slings',
    shortDescription: 'Shop our collection of lightweight and comfortable slings from our top selling collection Alpha Bravo, Voyageur, & More.',
  },
]);


const backpackFeatured = (id: number) => ([
  {
    id: id++,
    name: 'What\'s New',
    shortDescription: 'Our collection of modern, durable and comfortable backpacks and sling bags are perfect for both professionals & students. Find laptop backpacks, weekend bags & more.',
  },
  {
    id: id++,
    name: 'Best Sellers',
    shortDescription: 'Shop Tumi\'s best selling backpacks. Check out trending items like our Alpha Bravo backpack and Alpha 2 Laptop Brief Pack®. Quickly purchase before they\’re gone.',
  },
  {
    id: id++,
    name: 'Personalization Shop',
    shortDescription: 'Add a personal touch to your Tumi travel and business backpacks by having it monogrammed with yours or your loved one\'s initials.',
  },
  {
    id: id++,
    name: 'Mercedes-AMG Petronas Motorsport',
    shortDescription: 'TUMI has partnered with Mercedes-AMG Petronas Motorsport to share our passion which TUMI was founded: design excellence, technological innovation and superior functionality.',
  },
]);

const backpacks = (id: number) => ([
  {
    id: id++,
    name: 'Shop By Category',
    children: backpackCats(id),
    categoryQuery: 'PRODUCTTYPE',
    withViewAll: true,
    skipLink: true,
  },
  {
    id: id++,
    name: 'Featured',
    children: backpackFeatured(id),
    withViewAll: false,
    skipLink: true,
  },
]);

const bagsCats = (id: number) => ([
  {
    id: id++,
    name: 'Briefcases',
    shortDescription: 'Travel to work both lightly and stylishly with our collection of men\'s and women\'s briefcases and portfolios.',
  },
  {
    id: id++,
    name: 'Wheeled Briefcasas',
    shortDescription: 'Our collection of wheeled briefcases are both versatile and compact making business traveling a breeze. Find rolling backpacks, wheeled laptop bags & more.',
  },
  {
    id: id++,
    name: 'Crossbodies',
    shortDescription: 'Our crossbody bags are both adjustable and sleek. Perfectly fit all of your documents, laptops, business cards and other essentials.',
  },
  {
    id: id++,
    name: 'Messenger Bags',
    shortDescription: 'Our collection of messenger bags are both adjustable and lightweight. Perfectly fits all the essentials for men and women on the go.',
  },
  {
    id: id++,
    name: 'Totes',
    shortDescription: 'Shop our collection of sophisticated and sleek tote bags and travel duffels.',
  },
]);

const bagsFeatured = (id: number) => ([
  {
    id: id++,
    name: 'What\'s New',
    shortDescription: 'Shop TUMI’s collection of travel and business bags. We offer the highest quality messenger bags, cross body bags, briefcases & more.',
  },
  {
    id: id++,
    name: 'Best Sellers',
    shortDescription: 'Shop Tumi\'s best selling travel bags. Check out hot items like our Voyageur Totes and Alpha 2 Laptop briefs made from lightweight yet durable materials.',
  },
  {
    id: id++,
    name: 'Personalization Shop',
    shortDescription: 'Add a unique touch to your business and travel bags with complimentary personalization, monograms & unique color accents.',
  },
  {
    id: id++,
    name: 'TUMI X Orlebar Brown',
    shortDescription: '',
  },
]);

const bags = (id: number) => ([
  {
    id: id++,
    name: 'Shop By Category',
    categoryQuery: 'PRODUCTTYPE',
    children: bagsCats(id),
    withViewAll: true,
    skipLink: true,
  },
  {
    id: id++,
    name: 'Featured',
    children: bagsFeatured(id),
    withViewAll: false,
    skipLink: true,
  },
]);

const accessoriesCats = (id: number) => ([
  {
    id: id++,
    name: 'Wallets & Card Cases',
    shortDescription: 'Shop our broad selection of travel wallets, money clips and card cases made of carbon fiber, leather and other premium materials.',
  },
  {
    id: id++,
    name: 'Passport Cases & Covers',
    shortDescription: 'Shop our full collection of passport cases and covers to help organize your travel. Our passport cases are made from TUMI durable materials including ballistic, carbon fiber, and leather.',
  },
  {
    id: id++,
    name: 'Electronics',
    shortDescription: 'Stay fully charged and up to date with our TUMI tech accessories and electronics.',
  },
  {
    id: id++,
    name: 'Mobile Accessories',
    shortDescription: 'Protect your devices with our tech accessories. Shop for phone cases, tablet & laptop cases & other tech travel essentials.',
  },
  {
    id: id++,
    name: 'Travel Essentials',
    shortDescription: 'From compact travel kits to leather luggage tags, find the best travel accessories here at TUMI.',
  },
  {
    id: id++,
    name: 'Belts',
    shortDescription: 'Look sharp and elegant with our collection of men\'s belts made from leather & ballistic. The perfect finish to any outfit.',
  },
  {
    id: id++,
    name: 'Outerwear',
    shortDescription: 'Stay warm on any trip with our collection of men\'s and women\'s high tech, foldable jackets and outerwear.',
  },
  {
    id: id++,
    name: 'Key Fobs',
    shortDescription: 'Our multifunctional keyfobs and keychains are perfect for those constantly on the move.',
  },
  {
    id: id++,
    name: 'Eyewear',
    shortDescription: '',
  },
]);

const accessoriesFeatured = (id: number) => ([
  {
    id: id++,
    name: 'What\'s New',
    shortDescription: 'Shop TUMI’s accessories. We offer the highest quality travel accessories, mobile accessories, wallets, money clips & more.',
  },
  {
    id: id++,
    name: 'Best Sellers',
    shortDescription: 'Shop Tumi\'s best selling travel accessories, essentials and wallets. Check out hot items & quickly purchase before they’re gone.',
  },
  {
    id: id++,
    name: 'Personalization Shop',
    shortDescription: 'Make your mark on your next travel accessories purchase. With every order, TUMI will make your luggage and accessories stand out from the rest.',
  },
  {
    id: id++,
    name: 'Tumi ID Lock™',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'iPhone 7 & 7 Plus Cases',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'Wireless Innovations',
    shortDescription: 'Stay fully charged and up to date with our TUMI tech accessories and electronics.',
  },
]);

const accessories = (id: number) => ([
  {
    id: id++,
    name: 'Shop By Category',
    children: accessoriesCats(id),
    categoryQuery: 'PRODUCTTYPE',
    withViewAll: true,
    skipLink: true,
  },
  {
    id: id++,
    name: 'Featured',
    children: accessoriesFeatured(id),
    withViewAll: false,
    skipLink: true,
  },
]);

const collectionsCats = (id: number) => ([
  {
    id: id++,
    name: '19 Degree Aluminum',
    shortDescription: '',
  },
  {
    id: id++,
    name: '19 Degree Polycarbonate',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'Alpha Accessories',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'Alpha 2',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'Alpha Bravo',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'Arrivé',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'Ashton',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'Camden',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'CFX',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'Chambers',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'Harrison',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'Landon',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'Larkin',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'Mariella',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'Mason',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'Monaco',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'Sinclair',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'Tahoe',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'Tegra Lite®',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'Tegra Lite® X Frame',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'TUMI PAX',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'TUMI V3',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'Voyageur',
    shortDescription: '',
  },
  {
    id: id++,
    name: 'Weekend',
    shortDescription: '',
  },
]);

const collections = (id: number) => ([
  {
    id: id++,
    name: 'Shop By Collection',
    children: collectionsCats(id),
    withViewAll: false,
    overrideChildrenLink: '/c/$slug',
  },
]);

const saleCats = (id: number) => ([
  {
    id: id++,
    name: 'Luggage Sale',
  },
  {
    id: id++,
    name: 'Backpack Sale',
  },
  {
    id: id++,
    name: 'Bags Sale',
  },
  {
    id: id++,
    name: 'Accessories Sale',
  },
  {
    id: id++,
    name: 'Wallet Sale',
  },
  {
    id: id++,
    name: 'Outerwear Sale',
  },
]);

const saleFeatured = (id: number) => ([
  {
    id: id++,
    name: 'Best Sellers',
  },
  {
    id: id++,
    name: 'New Arrivals',
  },
  {
    id: id++,
    name: 'Alpha 2 Sale',
  },
  {
    id: id++,
    name: 'Astor Sale',
  },
  {
    id: id++,
    name: 'Tahoe Sale',
  },
  {
    id: id++,
    name: 'Voyageur Sale',
  },
]);

const sale = (id: number) => ([
  {
    id: id++,
    name: 'Shop By Category',
    children: saleCats(id),
    withViewAll: true,
    skipLink: true,
  },
  {
    id: id++,
    name: 'Featured',
    children: saleFeatured(id),
    withViewAll: false,
    skipLink: true,
  },
]);

const collectionLink = (name) => {
  return {
    name: 'category',
    params: {
      categoryName: categoryNameToUrl(name),
    },
  };
};

const luggageCollections = [
  {
    name: 'Ashton',
    image: 'https://cdn-media.amplience.com/tumi/images/333159GRY_ashton_lenox_600x600_1.jpg',
    linkTo: collectionLink('Ashton'),
  },
  {
    name: 'Tahoe',
    image: 'https://cdn-media.amplience.com/tumi/images/798660OR_tahoe_sierra_600x600.jpg',
    linkTo: collectionLink('Tahoe'),
  },
  {
    name: 'Arrivé',
    image: 'https://cdn-media.amplience.com/tumi/images/255061PW2_arrive_raleigh_600x600.jpg',
    linkTo: collectionLink('Arrivé'),
  },
  {
    name: 'TUMI V3',
    image: 'https://cdn-media.amplience.com/tumi/images/228060PAC_v3_intlcarryon_600x600.jpg',
    linkTo: collectionLink('TUMI V3'),
  },
  {
    name: 'Alpha 2',
    image: 'https://cdn-media.amplience.com/tumi/images/22636D2_alpha2_4wheeledgarmentbag_600x600.jpg',
    linkTo: collectionLink('Alpha 2'),
  },
  {
    name: 'Larkin',
    image: 'https://cdn-media.amplience.com/tumi/images/73760GBSP_larkin_sam_600x600.jpg',
    linkTo: collectionLink('Larkin'),
  },
];

const backpacksCollections = [
  {
    name: 'Arrivé',
    image: 'https://cdn-media.amplience.com/tumi/images/255012D2_bradley_arrive%C3%8C%C2%81_600x600.jpg',
    linkTo: collectionLink('Arrivé'),
  },
  {
    name: 'Tahoe',
    image: 'https://cdn-media.amplience.com/tumi/images/798645LC_tahoe_covebackpack_600x600.jpg',
    linkTo: collectionLink('Tahoe'),
  },
  {
    name: 'Sinclair',
    image: 'https://cdn-media.amplience.com/tumi/images/79380EG_sinclair_oliviabackpack_600x600.jpg',
    linkTo: collectionLink('Sinclair'),
  },
  {
    name: 'Alpha 2',
    image: 'https://cdn-media.amplience.com/tumi/images/26177EG2_alpha2_slimbriefbackpack_600x600.jpg',
    linkTo: collectionLink('Alpha 2'),
  },
  {
    name: 'Voyageur',
    image: 'https://cdn-media.amplience.com/tumi/images/484758PNK_voyageur_halle_600x600.jpg',
    linkTo: collectionLink('Voyageur'),
  },
  {
    name: 'Alpha Bravo',
    image: 'https://cdn-media.amplience.com/tumi/images/92682DSK2_alphabravo_doverleather_600x600.jpg',
    linkTo: collectionLink('Alpha Bravo'),
  },
];

const bagsCollections = [
  {
    name: 'Arrivé',
    image: 'https://cdn-media.amplience.com/tumi/images/955002D2_arrive_sawyerbrief_600x600_1.jpg',
    linkTo: collectionLink('Arrivé'),
  },
  {
    name: 'Ashton',
    image: 'https://cdn-media.amplience.com/tumi/images/933253GRN_ashton_cypressbrief_600x600.jpg',
    linkTo: collectionLink('Ashton'),
  },
  {
    name: 'Sinclair',
    image: 'https://cdn-media.amplience.com/tumi/images/79391D_sinclair_ninacommuter_600x600.jpg',
    linkTo: collectionLink('Sinclair'),
  },
  {
    name: 'Alpha 2',
    image: 'https://cdn-media.amplience.com/tumi/images/26516EG2_alpha2_tpassbrief_600x600_1.jpg',
    linkTo: collectionLink('Alpha 2'),
  },
  {
    name: 'Voyageur',
    image: 'https://cdn-media.amplience.com/tumi/images/494768CDT_voyageur_lolamessenger_600x600_1.jpg',
    linkTo: collectionLink('Voyageur'),
  },
  {
    name: 'Harrison',
    image: 'https://cdn-media.amplience.com/tumi/images/63000GRY_harrison_senecabrief_600x600.jpg',
    linkTo: collectionLink('Harrison'),
  },
];

const accessoriesCollections = [
  {
    name: 'Electronics',
    image: 'https://cdn-media.amplience.com/tumi/images/114303D_electronics_wirelessbuds_600x600_1.jpg',
    linkTo: collectionLink('Electronics'),
  },
  {
    name: 'Mobile Accessories',
    image: 'https://cdn-media.amplience.com/tumi/images/114227DSLV_mobileelectronics_twopiece_600x600.jpg',
    linkTo: collectionLink('Mobile Accessories'),
  },
  {
    name: 'Belts',
    image: 'https://cdn-media.amplience.com/tumi/images/15975GMDD_belts_leather_600x600.jpg',
    linkTo: collectionLink('Belts'),
  },
  {
    name: 'Sinclair',
    image: 'https://cdn-media.amplience.com/tumi/images/43311MB2_sinclair_trifoldwallet_600x600.jpg',
    linkTo: collectionLink('Sinclair'),
  },
  {
    name: 'Camdem',
    image: 'https://cdn-media.amplience.com/tumi/images/11824WSK_camden_smallclutchfolio_600x600.jpg',
    linkTo: collectionLink('Camdem'),
  },
  {
    name: 'Monaco',
    image: 'https://cdn-media.amplience.com/tumi/images/119860GRYID_monaco_slimcard_600x600.jpg',
    linkTo: collectionLink('Monaco'),
  },
];

const featuredCollections = [
  {
    name: 'Alpha 2',
    description: 'Sophisticated travel and business accessories crafted from our signature FXT ballistic nylon® for the utmost durability.',
    image: 'https://cdn-media.amplience.com/tumi/images/FW16_Alpha2_Ballistic_Travel_72dpi.jpg',
    linkTo: collectionLink('Alpha 2'),
    color: 'white',
    titleOnTile: true,
  },
  {
    name: 'Arrivé',
    description: 'Meticulously appointed with the finest aesthetic and engineering features.',
    image: 'https://cdn-media.amplience.com/tumi/images/ECOM_Arrive_FINALFOCUS.jpg',
    linkTo: collectionLink('Arrivé'),
    color: 'white',
    titleOnTile: true,
  },
  {
    name: 'Alpha Bravo',
    description: 'TUMI\'s landmark collection featuring a versatile array of more casual business and travel designs.',
    image: 'https://cdn-media.amplience.com/tumi/images/ECOM_Alpha_Bravo_Grey_FINALFOCUS1.jpg',
    linkTo: collectionLink('Alpha Bravo'),
    color: 'white',
    titleOnTile: true,
  },
  {
    name: 'Sinclair',
    description: 'A modern collection of thoughtfully-designed tote bags, briefs, and travel accessories for women on the move.',
    image: 'https://cdn-media.amplience.com/tumi/images/ECOM_Sinclair_FINALFOCUS.jpg',
    linkTo: collectionLink('Sinclair'),
    color: 'black',
    titleOnTile: true,
    imageClass: 'scaled',
  },
  {
    name: 'V3',
    description: 'Our lightest luggage. Ever. View the Collection Comparison Chart',
    image: 'https://cdn-media.amplience.com/tumi/images/ECOM_V3_1481.jpg',
    linkTo: collectionLink('V3'),
    color: 'black',
    titleOnTile: false,
  },
  {
    name: 'Voyageur',
    description: 'Our most colorful line of travel, business and everyday designs for women.',
    image: 'https://cdn-media.amplience.com/tumi/images/ECOM_Voyageur_FINALFOCUS.jpg',
    linkTo: collectionLink('Voyageur'),
    color: 'black',
    titleOnTile: false,
  },
];

let categoryId = 0;
const categories = [
  {
    id: categoryId++,
    name: 'luggage',
    description: 'TUMI luggage, carry-ons, duffels and bestselling garment bags are strong, lightweight and engineered to endure.',
    showNameCatPage: true,
    isHighlighted: false,
    children: luggage(categoryId),
    imgWidth: 50,
    imgSrc: 'https://i1.adis.ws/i/tumi/03.30.17_luggage_600x320?w=600&h=320',
    linkTo: `/categories/${categoryNameToUrl('luggage')}`,
    heroImage: 'https://cdn-media.amplience.com/tumi/images/03.30.17_CategoryBanners_travel_1600x538.jpg',
    collections: luggageCollections,
    position: 'right',
    shortDescription: 'Our travel luggage is unparalleled in reliability, beauty & functionality. Shop our carry-ons, checked luggage, duffel bags & more.',
  },
  {
    id: categoryId++,
    name: 'backpacks',
    description: 'TUMI backpacks are strong, light in weight and digitally driven for sophisticated modern-day business travelers.',
    showNameCatPage: true,
    isHighlighted: false,
    children: backpacks(categoryId),
    imgWidth: 50,
    imgSrc: 'https://i1.adis.ws/i/tumi/03.30.17_backpacks_600x320?w=600&h=320',
    linkTo: `/categories/${categoryNameToUrl('backpacks')}`,
    heroImage: 'https://cdn-media.amplience.com/tumi/images/03.30.17_CategoryBanners_backpacks_1600x538_3.jpg',
    collections: backpacksCollections,
    position: 'right',
    shortDescription: 'Our collection of modern, durable and comfortable backpacks and sling bags are perfect for both professionals & students. Find laptop backpacks, weekend bags & more.',
  },
  {
    id: categoryId++,
    name: 'bags',
    description: 'Strong, durable and innovative TUMI duffels, tote bags and briefcases.',
    showNameCatPage: true,
    isHighlighted: false,
    children: bags(categoryId),
    imgWidth: 50,
    imgSrc: 'https://i1.adis.ws/i/tumi/03.30.17_bags_600x320?w=600&h=320',
    linkTo: `/categories/${categoryNameToUrl('bags')}`,
    heroImage: 'https://cdn-media.amplience.com/tumi/images/03.30.17_CategoryBanners_bags_1600x538_1.jpg',
    collections: bagsCollections,
    position: 'left',
    shortDescription: 'Shop TUMI’s collection of travel and business bags. We offer the highest quality messenger bags, cross body bags, briefcases & more.',
  },
  {
    id: categoryId++,
    name: 'accessories',
    description: 'Electronics, leather wallets, iPhone cases, passport cases and outerwear to complete your journey.',
    showNameCatPage: true,
    isHighlighted: false,
    children: accessories(categoryId),
    imgWidth: 35,
    imgSrc: 'https://i1.adis.ws/i/tumi/03.30.17_accessories_420x320?w=420&h=320',
    linkTo: `/categories/${categoryNameToUrl('accessories')}`,
    heroImage: 'https://cdn-media.amplience.com/tumi/images/03.30.17_CamdenHero_1600x670_2.jpg',
    collections: accessoriesCollections,
    position: 'left',
    shortDescription: 'Shop TUMI’s accessories. We offer the highest quality travel accessories, mobile accessories, wallets, money clips & more.',
  },
  {
    id: categoryId++,
    name: 'collections',
    description: '',
    showNameCatPage: true,
    isHighlighted: false,
    children: collections(categoryId),
    imgWidth: 25,
    imgSrc: 'https://i1.adis.ws/i/tumi/01.05.17_Nav_Collections19D_300x320?w=300&h=320',
    linkTo: `/categories/${categoryNameToUrl('collections')}`,
    heroImage: 'https://cdn-media.amplience.com/tumi/images/v3_test_bg_1650x616_1.jpg',
    collections: featuredCollections,
  },
  {
    id: categoryId++,
    name: 'sale',
    description: '',
    showNameCatPage: true,
    isHighlighted: true,
    children: sale(categoryId),
    imgWidth: 50,
    imgSrc: 'https://i1.adis.ws/i/tumi/SemiAnnualSale_600x320?w=600&h=320',
  },
];

const initialState = {
  list: [],
};

const {fetch, ...actions} = createAsyncActions(
  'categories',
  () => Promise.resolve(categories)
);

const reducer = createReducer({
  [actions.succeeded]: (state, payload) => {
    return {
      ...state,
      list: payload,
    };
  },
}, initialState);

export {
  reducer as default,
  fetch,
  categories,
};
