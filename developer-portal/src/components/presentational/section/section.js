import React, { Component } from 'react';
import './section.css';

class Section extends Component {
  static propTypes = {
    title: React.PropTypes.string,
  }

  render() {
    const { children, title } = this.props;

    return (
      <div className="section">
        {title && (
          <div className="section__title-block">
            <h1 className="section__title">{title}</h1>
            <div className="section__buffer"></div>
          </div>
        )}
        <div className="section__content">
          {children}
        </div>
      </div>
    )
  }
}

export default Section;
