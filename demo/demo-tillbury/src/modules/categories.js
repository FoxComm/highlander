/* @flow */
/* eslint no-param-reassign: 0 */
/* eslint max-len: 0 */

import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import { categoryNameToUrl } from 'paragons/categories';


const face = (id: number) => ([
  {
    id: id++,
    name: 'FOUNDATION',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKEUP-PRODUCT-CATEGORY-Foundation_1_.jpg',
  },
  {
    id: id++,
    name: 'POWDER',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKEUP-PRODUCT-CATEGORY-FACE-POWDER.jpg',
  },
  {
    id: id++,
    name: 'CONCEALER',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKEUP-PRODUCT-CATEGORY-FACE-CONCEALER.jpg',
  },
  {
    id: id++,
    name: 'FACE KITS',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKEUP-PRODUCT-CATEGORY-FaceKits_1_.jpg',
  },
  {
    id: id++,
    name: 'FACE PALLETES',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKEUP-PRODUCT-CATEGORY-FACE-PALETTE.jpg',
  },
  {
    id: id++,
    name: 'ALL FACE',
    linkTo: '/c/makeup/face',
  },
  {
    id: id++,
    name: 'FOUNDATION FINDER',
    linkTo: '/',
  },
]);

const cheek = (id: number) => ([
  {
    id: id++,
    name: 'BLUSH',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKEUP-PRODUCT-CATEGORY-Blusher_1_.jpg',
  },
  {
    id: id++,
    name: 'CONTOUR',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKEUP-PRODUCT-CATEGORY-CHEEK-CONTOUR.jpg',
  },
  {
    id: id++,
    name: 'BRONZER',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKEUP-PRODUCT-CATEGORY-CHEEK-BRONZER.jpg',
  },
  {
    id: id++,
    name: 'HIGHLIGHTER',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKEUP-PRODUCT-CATEGORY-Highlighter_1_.jpg',
  },
  {
    id: id++,
    name: 'PALLETE',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/PRODUCT-CATEGORY-FILMSTARS-ON-THE-GO-V3.jpg',
  },
  {
    id: id++,
    name: 'CHEEK KITS',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKEUP-PRODUCT-CATEGORY-CHEEK-KITS.jpg',
  },
  {
    id: id++,
    name: 'ALL CHEEK',
    linkTo: '/c/makeup/cheek',
  },
]);

const eyes = (id: number) => ([
  {
    id: id++,
    name: 'MASCARA',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKEUP-PRODUCT-CATEGORY-mascara_1_.jpg',
  },
  {
    id: id++,
    name: 'EYELINER',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKEUP-PRODUCT-CATEGORY-EYES-EYELINER.jpg',
  },
  {
    id: id++,
    name: 'EYESHADOW',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKEUP-PRODUCT-CATEGORY-Eyeshadow_1_.jpg',
  },
  {
    id: id++,
    name: 'EYEBROW',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKEUP-PRODUCT-CATEGORY-EYES-EYEBROWS.jpg',
  },
  {
    id: id++,
    name: 'PALLETE',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/PRODUCT-CATEGORY-PALETTE_4.jpg',
  },
  {
    id: id++,
    name: 'EYE MAKEUP REMOVER',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/PRODUCT-CATEGORY-REMOVER_2.jpg',
  },
  {
    id: id++,
    name: 'EYE KIT',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKEUP-PRODUCT-CATEGORY-EYES-EYEKITS_1.jpg',
  },
  {
    id: id++,
    name: 'ALL EYE',
    linkTo: '/c/makeup/eyes',
  },
]);

const lips = (id: number) => ([
  {
    id: id++,
    name: 'LIPSTICK',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKEUP-PRODUCT-CATEGORY-LIPS-LIPSTICK.jpg',
  },
  {
    id: id++,
    name: 'LIP LINER',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKEUP-PRODUCT-CATEGORY-Lipliner_1_.jpg',
  },
  {
    id: id++,
    name: 'LIP GLOSS',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKEUP-PRODUCT-CATEGORY-LipGloss2_1_.jpg',
  },
  {
    id: id++,
    name: 'LIP SCRUB',
    catalogImage: '',
  },
  {
    id: id++,
    name: 'LIP KITS',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKEUP-PRODUCT-CATEGORY-LipKits.jpg',
  },
  {
    id: id++,
    name: 'ALL LIPS',
    linkTo: '/c/makeup/lips',
  },
  {
    id: id++,
    name: 'LIPSTICK FINDER',
    linkTo: '/',
  },
]);

const tools = (id: number) => ([
  {
    id: id++,
    name: 'BRUSHES',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKE-UP-SUB-CATEGORY-TOOLS_5.jpg',
  },
  {
    id: id++,
    name: 'MAKEUP BAGS',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/PRODUCT-CATEGORY-MAKEUP-BAGS.jpg',
  },
  {
    id: id++,
    name: 'EYELASH CURLER',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/PRODUCT-CATEGORY-EYELASH-CURLERS.jpg',
  },
  {
    id: id++,
    name: 'PENCIL SHARPENER',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/PRODUCT-CATEGORY-SHARPENER.jpg',
  },
  {
    id: id++,
    name: 'ALL TOOLS',
    linkTo: '/c/makeup/tools',
  },
]);

const makeupCollections = (id: number) => ([
  {
    id: id++,
    name: '#GLOWMO',
    linkTo: '/',
  },
  {
    id: id++,
    name: 'WEDDING MAKEUP',
    linkTo: '/',
  },
  {
    id: id++,
    name: 'QUICK \'N\' EASY',
    linkTo: '/',
  },
  {
    id: id++,
    name: 'THE DREAMY LOOK IN A CLUTCH',
    linkTo: '/',
  },
  {
    id: id++,
    name: 'MAGIC FOUNDATION',
    linkTo: '/',
  },
  {
    id: id++,
    name: 'LEGENDARY BROWS',
    linkTo: '/',
  },
  {
    id: id++,
    name: 'HOT LIPS',
    linkTo: '/',
  },
]);

const makeup = (id: number) => ([
  {
    id: id++,
    name: 'FACE',
    children: face(id),
    images: [
      'http://www.charlottetilbury.com/media/wysiwyg/MAKEUP-CATEGORY-PAGE-FACE01-349X371.jpg',
      'http://www.charlottetilbury.com/media/wysiwyg/MAKEUP-CATEGORY-PAGE-FACE02-349X371.jpg',
    ],
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKE-UP-SUB-CATEGORY-FACE_8.jpg',
  },
  {
    id: id++,
    name: 'CHEEK',
    children: cheek(id),
    images: [
      'http://www.charlottetilbury.com/media/wysiwyg/MAKEUP-CATEGORY-PAGE-CHEEK01-349X371_1.jpg',
      'http://www.charlottetilbury.com/media/wysiwyg/MAKEUP-CATEGORY-PAGE-CHEEK02-349X371_1.jpg',
    ],
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKE-UP-SUB-CATEGORY-CHEEK_4.jpg',
  },
  {
    id: id++,
    name: 'EYES',
    children: eyes(id),
    images: [
      'http://www.charlottetilbury.com/media/wysiwyg/MAKEUP-CATEGORY-PAGE-EYES01--349X371.jpg',
      'http://www.charlottetilbury.com/media/wysiwyg/MAKEUP-CATEGORY-PAGE-EYES02-349X371_3.jpg',
    ],
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/Charlotte_Tilbury_Eyeshadow_Luxury_Palettes_1.jpg',
  },
  {
    id: id++,
    name: 'LIPS',
    children: lips(id),
    images: [
      'http://www.charlottetilbury.com/media/wysiwyg/MAKEUP-CATEGORY-PAGE-LIPS02-349X371.jpg',
      'http://www.charlottetilbury.com/media/wysiwyg/MAKEUP-CATEGORY-PAGE-LIPS01-349X371.jpg',
    ],
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKE-UP-SUB-CATEGORY-LIPS_7.jpg',
  },
  {
    id: id++,
    name: 'TOOLS',
    children: tools(id),
    images: [
      'http://www.charlottetilbury.com/media/wysiwyg/MAKEUP-CATEGORY-PAGE-TOOLS01-349X371.jpg',
      'http://www.charlottetilbury.com/media/wysiwyg/MAKEUP-CATEGORY-PAGE-TOOLS02-349X371.jpg',
    ],
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKEUP-PRODUCT-CATEGORY-Tools.jpg',
  },
  {
    id: id++,
    name: 'BEAUTY COLLECTIONS',
    children: makeupCollections(id),
    images: [
      'http://www.charlottetilbury.com/media/wysiwyg/CATEGORY-BEAUTY-COLLECTIONS---698X371.jpg',
    ],
  },
]);

const skincareFace = (id: number) => ([
  {
    id: id++,
    name: 'MASK',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/SKINCARE-PRODUCT-CATEGORY-MASK_5.jpg',
  },
  {
    id: id++,
    name: 'MOISTURIZER',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/SKINCARE-PRODUCT-CATEGORY-MOISTURISER_7.jpg',
  },
  {
    id: id++,
    name: 'SKIN CARE PRIMER',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/SKINCARE-PRODUCT-CATEGORY-PRIMER_1.jpg',
  },
  {
    id: id++,
    name: 'EYE CREAM',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/SKINCARE-PRODUCT-CATEGORY-EYE-CREAM_4.jpg',
  },
  {
    id: id++,
    name: 'NIGHT CREAM',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/SKINCARE-PRODUCT-CATEGORY-NIGHT-CREAM_3.jpg',
  },
  {
    id: id++,
    name: 'SKIN CARE KITS',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/SKINCARE-PRODUCT-CATEGORY-SKIN-CARE-KITS.jpg',
  },
  {
    id: id++,
    name: 'FIND YOUR SKIN SOLUTION',
    linkTo: '/',
  },
]);

const scent = (id: number) => ([
  {
    id: id++,
    name: '30 ML',
  },
  {
    id: id++,
    name: '50 ML',
  },
  {
    id: id++,
    name: '100 ML',
  },
]);

const perfume = (id: number) => ([
  {
    id: id++,
    name: 'SCENT OF A DREAM',
    children: scent(id),
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/03.-FRAGRANCE-PRODUCT-CATEGORY-_TRIAL-SIZE__1.jpg',
  },
]);

const skincareBody = (id: number) => ([
  {
    id: id++,
    name: 'SUPERMODEL BODY',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/SKINCARE-PRODUCT-CATEGORY-SUPERMODEL-BODY_3.jpg',
  },
  {
    id: id++,
    name: 'BODY BALM',
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/SKINCARE-PRODUCT-CATEGORY-BODY-BALM_3.jpg',
  },
]);

const skincare = (id: number) => ([
  {
    id: id++,
    name: 'FACE',
    children: skincareFace(id),
    images: [
      'http://www.charlottetilbury.com/media/wysiwyg/SKINCARE-CATEGORY-PAGE-FACE-1_4.jpg',
      'http://www.charlottetilbury.com/media/wysiwyg/SKINCARE-CATEGORY-PAGE-FACE-2_2.jpg',
    ],
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/MAKE-UP-SUB-CATEGORY-FACE_8.jpg',
  },
  {
    id: id++,
    name: 'BODY',
    children: skincareBody(id),
    images: [
      'http://www.charlottetilbury.com/media/wysiwyg/SKINCARE-CATEGORY-PAGE-BODY-1_4.jpg',
      'http://www.charlottetilbury.com/media/wysiwyg/SKINCARE-CATEGORY-PAGE-BODY-2_3.jpg',
    ],
    catalogImage: 'http://media.charlottetilbury.com/catalog/category/SKINCARE-SUB-CATEGORY-BODY_4.jpg',
  },
]);

const byPrice = (id: number) => ([
  {
    id: id++,
    name: 'TRINKETS UNDER $55',
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/TRINKETS_1_.jpg',
  },
  {
    id: id++,
    name: 'TREATS UNDER $130',
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/TREATS_1_.jpg',
  },
  {
    id: id++,
    name: 'TREASURES OVER $130',
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/TREASURES_1_.jpg',
  },
]);

const byCategory = (id: number) => ([
  {
    id: id++,
    name: 'SKIN CARE KITS',
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/SKINCARE_4.jpg',
  },
  {
    id: id++,
    name: 'FACE KITS',
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/FACE-KITS.jpg',
  },
  {
    id: id++,
    name: 'EYE KITS',
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/EYE-KITS.jpg',
  },
  {
    id: id++,
    name: 'LIP KITS',
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/LIP-KITS.jpg',
  },
  {
    id: id++,
    name: 'TOOL KITS',
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/TOOL-KITS.jpg',
  },
  {
    id: id++,
    name: 'TRAVEL ESSENTIALS',
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/TRAVEL-ESSENTIALS.jpg',
  },
  {
    id: id++,
    name: 'PERFUME',
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/PERFUME_1.jpg',
  },
]);

const byLook = (id: number) => ([
  {
    id: id++,
    name: 'THE INGENUE',
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/ingenue.jpg',
  },
  {
    id: id++,
    name: 'THE GOLDEN GODDESS',
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/GOLDEN-GODDESS.jpg',
  },
  {
    id: id++,
    name: 'THE UPTOWN GIRL',
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/UPTOWN-GIRL.jpg',
  },
  {
    id: id++,
    name: 'THE SOPHISTICATE',
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/SOPHISTICATE_1.jpg',
  },
  {
    id: id++,
    name: 'THE ROCK CHICK',
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/ROCK-CHICK.jpg',
  },
  {
    id: id++,
    name: 'THE DOLCE VITA',
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/DOLCE-VITA.jpg',
  },
  {
    id: id++,
    name: 'THE BOMBSHELL',
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/BOMBSHELL_1.jpg',
  },
  {
    id: id++,
    name: 'THE VINTAGE VAMP',
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/VINTAGE-VAMP.jpg',
  },
  {
    id: id++,
    name: 'THE GLAMOUR MUSE',
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/GLAMOUR-MUSE.jpg',
  },
  {
    id: id++,
    name: 'THE REBEL',
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/REBEL_1.jpg',
  },
]);

const gifts = (id: number) => ([
  {
    id: id++,
    name: 'BY PRICE',
    children: byPrice(id),
    images: [
      'http://www.charlottetilbury.com/media/wysiwyg/GIFTS-CATEGORY-PAGE-SHOP-BY-PRICE_1.jpg',
    ],
    linkTo: '/categories/gifts',
  },
  {
    id: id++,
    name: 'BY CATEGORY',
    children: byCategory(id),
    images: [
      'http://www.charlottetilbury.com/media/wysiwyg/GIFTS-CATEGORY-PAGE-SHOP-BY-CAT.jpg',
    ],
    linkTo: '/categories/gifts',
  },
  {
    id: id++,
    name: 'BY LOOK',
    children: byLook(id),
    images: [
      'http://www.charlottetilbury.com/media/wysiwyg/ONLUNE-EXCLUSIVES-CATEGORY-PAGE_1.jpg',
    ],
    linkTo: '/categories/gifts',
  },
]);

const lookCat = (id: number) => ([
  {
    id: id++,
    name: 'THE INGENUE',
    imgSrc: 'http://www.charlottetilbury.com/media/wysiwyg/THE-INGENUE-DD-MENU.jpg',
    width: 10,
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/ingenue.jpg',
  },
  {
    id: id++,
    name: 'THE GOLDEN GODDESS',
    imgSrc: 'http://www.charlottetilbury.com/media/wysiwyg/THE-GOLDEN-GODDESS-DD-MENU.jpg',
    width: 10,
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/GOLDEN-GODDESS.jpg',
  },
  {
    id: id++,
    name: 'THE UPTOWN GIRL',
    imgSrc: 'http://www.charlottetilbury.com/media/wysiwyg/THE-UPTOWN-GIRL-DD-MENU.jpg',
    width: 10,
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/UPTOWN-GIRL.jpg',
  },
  {
    id: id++,
    name: 'THE SOPHISTICATE',
    imgSrc: 'http://www.charlottetilbury.com/media/wysiwyg/THE-SOPHISTICATE-DD-MENU.jpg',
    width: 10,
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/SOPHISTICATE_1.jpg',
  },
  {
    id: id++,
    name: 'THE ROCK CHICK',
    imgSrc: 'http://www.charlottetilbury.com/media/wysiwyg/THE-ROCK-CHICK-DD-MENU.jpg',
    width: 10,
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/ROCK-CHICK.jpg',
  },
  {
    id: id++,
    name: 'THE DOLCE VITA',
    imgSrc: 'http://www.charlottetilbury.com/media/wysiwyg/THE-DOLCE-VITA-DD-MENU.jpg',
    width: 10,
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/DOLCE-VITA.jpg',
  },
  {
    id: id++,
    name: 'THE BOMBSHELL',
    imgSrc: 'http://www.charlottetilbury.com/media/wysiwyg/THE-BOMSHELL-DD-MENU.jpg',
    width: 10,
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/BOMBSHELL_1.jpg',
  },
  {
    id: id++,
    name: 'THE VINTAGE VAMP',
    imgSrc: 'http://www.charlottetilbury.com/media/wysiwyg/VINTAGE-VAMP-DD-MENU.jpg',
    width: 10,
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/VINTAGE-VAMP.jpg',
  },
  {
    id: id++,
    name: 'THE GLAMOUR MUSE',
    imgSrc: 'http://www.charlottetilbury.com/media/wysiwyg/THE-GLAMOUR-MUSE-DD-MENU.jpg',
    width: 10,
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/GLAMOUR-MUSE.jpg',
  },
  {
    id: id++,
    name: 'THE REBEL',
    imgSrc: 'http://www.charlottetilbury.com/media/wysiwyg/THE-GLAMOUR-MUSE-DD-MENU.jpg',
    width: 10,
    withoutTitle: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/REBEL_1.jpg',
  },
]);

const newItems = (id: number) => ([
  {
    id: id++,
    name: 'NEW',
    imgSrc: 'http://www.charlottetilbury.com/media/wysiwyg/OB_G-with-text.jpg',
    width: 20,
    heading: 'NEW',
    linkTo: '/c/new',
  },
  {
    id: id++,
    name: 'ONLINE EXCLUSIVES',
    heading: 'ONLINE EXCLUSIVE',
    imgSrc: 'http://www.charlottetilbury.com/media/wysiwyg/PRODUCT-DROPDOWN-GLOW-OMLINE-EXCLUSIVES.jpg',
    width: 20,
    heading: 'ONLINE EXCLUSIVE',
    linkTo: `/c/${categoryNameToUrl('ONLINE EXCLUSIVE')}`,
  },
  {
    id: id++,
    name: 'LIMITED EDITION',
    heading: 'LIMITED EDITION',
    imgSrc: 'http://www.charlottetilbury.com/media/wysiwyg/instant-glow-palette.jpg',
    width: 20,
    heading: 'LIMITED EDITION',
    linkTo: `/c/${categoryNameToUrl('LIMITED EDITION')}`,
  },
  {
    id: id++,
    name: 'BEST SELLERS',
    heading: 'BEST SELLERS',
    imgSrc: 'http://www.charlottetilbury.com/media/wysiwyg/PRODUCT-DROPDOWN-GLOW-BESTSELLERS.jpg',
    width: 20,
    heading: 'BEST SELLERS',
    linkTo: `/c/${categoryNameToUrl('BEST SELLERS')}`,
  },
  {
    id: id++,
    name: 'AWARD WINNERS',
    heading: 'AWARD WINNERS',
    imgSrc: 'http://www.charlottetilbury.com/media/wysiwyg/PRODUCT-DROPDOWN-AWARD-WINNERS_5.jpg',
    width: 20,
    heading: 'AWARD WINNERS',
    linkTo: `/c/${categoryNameToUrl('AWARD WINNERS')}`,
  },
]);

let categoryId = 0;
const categories = [
  {
    id: categoryId++,
    name: 'NEW',
    heading: 'NEW',
    showNameCatPage: true,
    isHighlighted: false,
    children: newItems(categoryId),
    imgWidth: 0,
    imgSrc: [],
    heroImage: null,
    collections: [],
    position: 'right',
    withoutViewAll: true,
  },
  {
    id: categoryId++,
    name: 'MAKEUP',
    description: 'TUMI backpacks are strong, light in weight and digitally driven for sophisticated modern-day business travelers.',
    showNameCatPage: true,
    isHighlighted: false,
    children: makeup(categoryId),
    imgWidth: 25,
    imgSrc: ['http://www.charlottetilbury.com/media/wysiwyg/GLOW-NAV-CONTENT-INSTANTPALETTE_V2.jpg'],
    linkTo: `/categories/${categoryNameToUrl('MAKEUP')}`,
    heroImage: 'https://cdn-media.amplience.com/tumi/images/03.30.17_CategoryBanners_backpacks_1600x538_3.jpg',
    collections: [],
    position: 'right',
    shortDescription: 'Our collection of modern, durable and comfortable backpacks and sling bags are perfect for both professionals & students. Find laptop backpacks, weekend bags & more.',
  },
  {
    id: categoryId++,
    name: 'SKIN CARE',
    description: 'Strong, durable and innovative TUMI duffels, tote bags and briefcases.',
    showNameCatPage: true,
    isHighlighted: false,
    children: skincare(categoryId),
    imgWidth: 50,
    imgSrc: ['http://www.charlottetilbury.com/media/wysiwyg/SKINCARE_5.jpg'],
    linkTo: `/categories/${categoryNameToUrl('SKIN CARE')}`,
    heroImage: 'https://cdn-media.amplience.com/tumi/images/03.30.17_CategoryBanners_bags_1600x538_1.jpg',
    collections: [],
    position: 'left',
    shortDescription: 'Shop TUMI’s collection of travel and business bags. We offer the highest quality messenger bags, cross body bags, briefcases & more.',
  },
  {
    id: categoryId++,
    name: 'PERFUME',
    description: 'Electronics, leather wallets, iPhone cases, passport cases and outerwear to complete your journey.',
    showNameCatPage: true,
    isHighlighted: false,
    children: perfume(categoryId),
    imgWidth: 70,
    imgSrc: ['http://www.charlottetilbury.com/media/wysiwyg/SOAD_1.jpg'],
    linkTo: `/categories/${categoryNameToUrl('PERFUME')}`,
    heroImage: 'https://cdn-media.amplience.com/tumi/images/03.30.17_CamdenHero_1600x670_2.jpg',
    collections: [],
    position: 'left',
    shortDescription: 'Shop TUMI’s accessories. We offer the highest quality travel accessories, mobile accessories, wallets, money clips & more.',
    withoutViewAll: true,
    catalogImage: 'http://www.charlottetilbury.com/media/wysiwyg/PERFUME_1.jpg',
    withoutTitle: true,
  },
  {
    id: categoryId++,
    name: 'LOOKS',
    description: '',
    showNameCatPage: true,
    isHighlighted: false,
    children: lookCat(categoryId),
    imgWidth: 0,
    imgSrc: ['https://i1.adis.ws/i/tumi/01.05.17_Nav_Collections19D_300x320?w=300&h=320'],
    linkTo: `/categories/${categoryNameToUrl('LOOKS')}`,
    heroImages: [],
    collections: [],
  },
  {
    id: categoryId++,
    name: 'GIFTS',
    description: '',
    showNameCatPage: true,
    isHighlighted: false,
    children: gifts(categoryId),
    imgWidth: 40,
    imgSrc: [
      'http://www.charlottetilbury.com/media/wysiwyg/GIFTS_DROPDOWN_300x300.jpg',
      'http://www.charlottetilbury.com/media/wysiwyg/GIFTS2_1.jpg',
    ],
    linkTo: `/categories/${categoryNameToUrl('GIFTS')}`,
  },
  {
    id: categoryId++,
    name: 'CHARLOTTE\'S UNIVERSE',
    description: '',
    showNameCatPage: true,
    isHighlighted: true,
    linkTo: `/categories/${categoryNameToUrl('CHARLOTTE\'S UNIVERSE')}`,
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
