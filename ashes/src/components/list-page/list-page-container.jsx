import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';

import { IndexLink } from 'components/link';
import PageNav from 'components/core/page-nav';
import { PageTitle } from '../section-title';

const ListPageContainer = props => {

  const localNav = !_.isEmpty(props.navLinks) ? (
    <PageNav>
      {props.navLinks.map(l => <IndexLink key={l.to + l.title} to={l.to}>{l.title}</IndexLink>)}
    </PageNav>
  ) : null;

  return (
    <div className="fc-list-page">
      <div className="fc-list-page-header">
        <PageTitle
          title={props.title}
          subtitle={props.subtitle}
          documentTitle={props.documentTitle}
          onAddClick={props.handleAddAction}
          addTitle={props.addTitle} />
        {localNav}
      </div>
      {props.children}
    </div>
  );
};

ListPageContainer.propTypes = {
  children: PropTypes.node,
  addTitle: PropTypes.string,
  handleAddAction: PropTypes.func,
  subtitle: PropTypes.node,
  title: PropTypes.string.isRequired,
  navLinks: PropTypes.array,
};

ListPageContainer.defaultProps = {
  addTitle: '',
  navLinks: [],
};

export default ListPageContainer;
