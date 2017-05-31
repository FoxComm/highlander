/* @flow */

import React from 'react';

// libs
import _ from 'lodash';

// components
import MultiSelectRow from 'components/table/multi-select-row';

type Props = {
  catalog: Catalog,
  columns?: Columns,
  params: Object,
}

const CatalogRow = (props: Props) => {
  const { catalog, columns, params } = props;

  const commonParams = {
    columns,
    row: catalog,
    setCellContents: (catalog: Object, field: string) => _.get(catalog, field),
    params,
  };

  return (
    <MultiSelectRow
      {...commonParams}
      linkTo="catalog-details"
      linkParams={{ catalogId: catalog.id }}
    />
  );
};

export default CatalogRow;
