// @flow

// libs
import React, { Element } from 'react';

// helpers
import { activeStatus, isArchived } from 'paragons/common';

// components
import MultiSelectRow from 'components/table/multi-select-row';
import RoundedPill from 'components/rounded-pill/rounded-pill';

// types
import type { TaxonomyResult } from 'paragons/taxonomy';

type Props = {
  taxonomy: TaxonomyResult,
  columns?: Array<Object>,
  params: Object,
};

const setCellContents = (taxonomy: TaxonomyResult, field: string) => {
  switch(field) {
    case 'state':
      return <RoundedPill text={activeStatus(taxonomy)} />;
    default:
      return taxonomy[field];
  }
};

const TaxonomyRow = (props: Props) => {
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
