// @flow

//libs
import React from 'react';
import classNames from 'classnames';

// style
import styles from './taxon-list-widget.css';

type Props = {
  taxons: TaxonsTree,
  activeTaxonId: string,
  handleTaxonClick: (id: number) => any,
  getTitle: (node: TaxonNode) => string,
};

export default ({ taxons, activeTaxonId, handleTaxonClick, getTitle }: Props) => (
  <div>
    {taxons.map((item: TaxonNode) => {
        const id = item.taxon.id;
        const active = (activeTaxonId === id.toString());
        const className = classNames(styles.item, { [styles.active]: active });

        return (
          <div className={className} onClick={() => handleTaxonClick(id)} key={id}>
            {getTitle(item)}
          </div>
        );
      }
    )}
  </div>
);


