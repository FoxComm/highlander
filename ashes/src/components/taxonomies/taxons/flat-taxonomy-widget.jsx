// @flow

//libs
import React, { Component } from 'react';
import classNames from 'classnames';

// style
import styles from './taxon-list-widget.css';

export default class FlatTaxonomyListWidget extends Component {

  get content(): Element {
    const { taxons, currentTaxon, handleTaxonClick} = this.props;

    return taxons.map((item) => {
        const active = (currentTaxon === item.taxon.id.toString());
        const className = classNames(styles['item'], { [styles.active]: active });

        return (
          <div
            className={className}
            onClick={() => handleTaxonClick(item.taxon.id)}
            key={item.taxon.id}
          >
            {item.taxon.attributes.name.v}
          </div>
        );
      }
    );
  }

  render() {
    return(
      <div>
        {this.content}
      </div>
    );
  }

}


