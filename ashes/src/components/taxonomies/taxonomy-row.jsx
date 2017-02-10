// @flow

// libs
import React, { Element } from 'react';


// components
import MultiSelectRow from 'components/table/multi-select-row';
import StatePill from 'components/object-page/state-pill';

// types
import type { Taxonomy } from 'paragons/taxonomy';

type Props = {
  taxonomy: Taxonomy,
  columns?: Array<Object>,
  params: Object,
};

const setCellContents = (taxonomy: Taxonomy, field: string) => {
  switch(field) {
    case 'state':
      return <StatePill object={taxonomy} />;
    default:
      return taxonomy[field];
  }
};

const TaxonomyRow = (props: Props): Element => {
  const { taxonomy, columns, params } = props;
  const commonParams = {
    columns,
    row: taxonomy,
    setCellContents,
    params,
  };

  return (
    <MultiSelectRow
      {...commonParams}
    />
  );
};

export default TaxonomyRow;
