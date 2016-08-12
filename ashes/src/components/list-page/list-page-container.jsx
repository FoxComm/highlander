import React, { PropTypes } from 'react';
import _ from 'lodash';

import { IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';
import { PageTitle } from '../section-title';

const ListPageContainer = props => {

  const localNav = !_.isEmpty(props.navLinks) ? (
    <LocalNav>
      {props.navLinks.map(l => <IndexLink key={l.to + l.title} to={l.to}>{l.title}</IndexLink>)}
    </LocalNav>
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
