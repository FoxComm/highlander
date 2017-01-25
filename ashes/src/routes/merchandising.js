/* @flow */
import React from 'react';

import FoxRouter from 'lib/fox-router';
import { frn } from 'lib/frn';

import TaxonomiesListPage from 'v2/containers/taxonomies/list-page';

import type { Claims } from 'lib/claims';

const getRoutes = (jwt: Object) => {
  const router = new FoxRouter(jwt);

  const taxonomyRoutes =
    router.read('taxonomies-base', { path: 'taxonomies', frn: frn.merch.taxonomy }, [
      router.read('taxonomies-list-pages', { component: TaxonomiesListPage }, [
        router.read('taxonomies', { isIndex: true }),
      ]),
    ]);

  return (
    <div>
      {taxonomyRoutes}
    </div>
  );
};

export default getRoutes;
