/* @flow */
import React from 'react';

import FoxRouter from 'lib/fox-router';
import { frn } from 'lib/frn';

import TaxonomiesListPage from 'components/taxonomies/taxonomies';
import TaxonomiesSearchableList from 'components/taxonomies/searchable-list';
import TaxonomyPage from 'components/taxonomies/taxonomy';
import TaxonomyDetails from 'components/taxonomies/details';

import TaxonPage from 'components/taxonomies/taxons/taxon';
import TaxonDetails from 'components/taxonomies/taxons/details';
import TaxonProducts from 'components/taxonomies/taxons/products';
import TaxonsListPage from 'components/taxonomies/taxons/taxons';

const getRoutes = (jwt: Object) => {
  const router = new FoxRouter(jwt);

  const taxonomyRoutes =
    router.read('taxonomies-base', { path: 'taxonomies', frn: frn.merch.taxonomy }, [
      router.read('taxonomies-list-pages', { component: TaxonomiesListPage }, [
        router.read('taxonomies', { component: TaxonomiesSearchableList, isIndex: true }),
      ]),

      router.read('taxonomy', { path: ':context/:taxonomyId', titleParam: ':taxonomyId', component: TaxonomyPage }, [
        router.read('taxonomy-details', { component: TaxonomyDetails, isIndex: true }),
        router.read('values', { path: 'values', component: TaxonsListPage, frn: frn.merch.taxon }),
      ]),

      // fake :context/:taxonomyId path w/o components to build breadcrumbs like Taxonomies -> 1 -> Value -> 1
      router.read('taxonomy', { path: ':context/:taxonomyId', titleParam: ':taxonomyId' }, [
        router.read('taxons-base', { path: 'values', title: 'Values', frn: frn.merch.taxonomy }, [
          router.read('taxon', {
            path: ':taxonId',
            titleParam: ':taxonId',
            component: TaxonPage,
            frn: frn.merch.taxon
          }, [
            router.read('taxon-details', { component: TaxonDetails, isIndex: true }),
            router.read('taxon-products', { path: 'products', component: TaxonProducts, title: 'Products' }),
          ]),
        ]),
      ])

    ]);

  return (
    <div>
      {taxonomyRoutes}
    </div>
  );
};

export default getRoutes;
