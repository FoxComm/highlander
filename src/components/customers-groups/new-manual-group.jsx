import React, { PropTypes } from 'react';
import SectionTitle from '../section-title/section-title';
import FormField from '../forms/formfield.jsx';
import Form from '../forms/form.jsx';
import { Link } from '../link';
import { transitionTo } from '../../route-helpers';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// todo: move base parts with manual groups to base component
export default class NewManualGroup extends React.Component {

  static propTypes = {
  };

  render () {
    return (
      <div className="fc-group-new">
        <div className="fc-grid">
          <header className="fc-customer-form-header fc-col-md-1-1">
            <h1 className="fc-title">
              New Manual Customer Group
            </h1>
            <Link to="groups-new-dynamic">or create a dynamic group</Link>
          </header>
          <article className="fc-col-md-1-1">
            <div className="fc-grid fc-grid-no-gutter">
            </div>
          </article>
        </div>
      </div>
    );
  }
}
