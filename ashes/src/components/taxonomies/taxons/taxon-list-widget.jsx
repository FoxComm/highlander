// @flow

// libs
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { transitionTo } from 'browserHistory';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

// components
import { AddButton } from 'components/common/buttons';

// actions
import { fetch as fetchTaxonomy } from 'modules/taxonomies/details';

// style
import styles from './taxon-list-widget.css';

class TaxonListWidget extends Component {

  componentDidMount() {
    const { id, context } = this.props;
    this.props.fetchTaxonomy(id, context);
  }

  transition(id: number|string) {
    transitionTo('taxon-details', {
      taxonomyId: this.props.id,
      context: this.props.context,
      taxonId: id
    });
  }

  @autobind
  handleTaxonClick(id: string) {
    const {currentTaxon } = this.props;

    if (currentTaxon !== id.toString()) {
      this.transition(id);

    }
  }

  @autobind
  handleAddButton() {
    if (this.props.currentTaxon !== 'new') {
      this.transition('new');
    }
  }

  get content() {
    const { taxons } = this.props.taxonomy;

    const children = taxons.map((item) => {
      const active = (this.props.currentTaxon === item.taxon.id.toString());
      const className = classNames(styles['item'], { [styles.active]: active });

       return (
         <div
           className={className}
           onClick={() => this.handleTaxonClick(item.taxon.id)}
           key={item.taxon.id}
         >
           {item.taxon.attributes.name.v}
         </div>
         );
       }
    );
    return children;
  }

  render () {
    const { taxonomy } = this.props;

    if (!taxonomy || taxonomy.hierarchical) {
      return null;
    }

    return (
      <div styleName="root">
        <div styleName="header">
          {taxonomy.attributes.name.v}
        </div>
          {this.content}
        <div styleName="footer">
          <AddButton className="fc-btn-primary" onClick={this.handleAddButton}>
            Value
          </AddButton>
        </div>
      </div>
    );
  }

}

const mapState = ( {taxonomies: { details } }) => ({
  taxonomy: details.taxonomy
});

export default connect(mapState, { fetchTaxonomy })(TaxonListWidget);
