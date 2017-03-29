// @flow

//libs
import React from 'react';
import classNames from 'classnames';

// style
import styles from './taxon-list-widget.css';

type Props = {
  taxons: TaxonsTree,
  activeTaxonId?: string,
  onClick: (id: number) => any,
  getTitle: (taxon: Taxon) => string,
};

export default ({ taxons, activeTaxonId, onClick, getTitle }: Props) => (
  <div>
    {taxons.map((item: TaxonNode) => {
        const id = item.node.id;
        const active = (activeTaxonId === id.toString());
        const className = classNames(styles.item, { [styles.active]: active });

        return (
          <div className={className} onClick={() => onClick(id)} key={id}>
            {getTitle(item.node)}
          </div>
        );
      }
    )}
  </div>
);


