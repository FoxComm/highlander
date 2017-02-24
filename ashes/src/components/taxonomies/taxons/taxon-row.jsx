// @flow

// libs
import React, { Element } from 'react';

// helpers
import { activeStatus, isArchived } from 'paragons/common';

// components
import MultiSelectRow from 'components/table/multi-select-row';
import RoundedPill from 'components/rounded-pill/rounded-pill';

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
      return 0;
    default:
      return taxon[field];
  }
};

const TaxonRow = (props: Props) => {
  const { taxon, columns, params } = props;
  const commonParams = {
    columns,
    row: taxon,
    setCellContents,
    params,
  };

  return <MultiSelectRow {...commonParams} />;

  // return (
  //   <MultiSelectRow
  //     {...commonParams}
  //     linkTo="taxonomy-details"
  //     linkParams={{taxonomyId: taxonomy.taxonomyId, context: taxonomy.context}}
  //   />
  // );
};

export default TaxonRow;
