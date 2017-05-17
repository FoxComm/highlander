// @flow

// libs
import React from 'react';

// helpers
import { activeStatus } from 'paragons/common';

// components
import MultiSelectRow from 'components/table/multi-select-row';
import { RoundedPill } from 'components/core/rounded-pill';

type Props = {
  taxon: TaxonResult,
  columns?: Array<Object>,
  params: Object,
};

const setCellContents = (taxon: TaxonResult, field: string) => {
  switch (field) {
    case 'state':
      return <RoundedPill text={activeStatus(taxon)} />;
    case 'productsCount':
      return 0; // TODO: fix after ES mapping update
    default:
      return taxon[field];
  }
};

const TaxonRow = ({ taxon, ...rest }: Props) => {
  return (
    <MultiSelectRow
      {...rest}
      row={taxon}
      setCellContents={setCellContents}
      linkTo="taxon-details"
      linkParams={taxon}
    />
  );
};

export default TaxonRow;
