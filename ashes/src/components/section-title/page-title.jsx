// @flow
import React, { Element, Component } from 'react';
import { setPageTitle } from 'lib/navigation';
import SectionTitle from './section-title';

/**
 * PageTitle is formally SectionTitle for defined parent context - big page headers.
 */

type Props = {
  children: Element<*>,
  title: string|Element<*>,
  documentTitle?: string|boolean;
}

export default class PageTitle extends Component {
  props: Props;

  componentDidMount() {
    const { props } = this;

    if (props.documentTitle !== false) {
      const documentTitle = props.documentTitle || props.title;
      if (typeof documentTitle == 'string') {
        setPageTitle(documentTitle);
      }
    }
  }

  render() {
    const { props } = this;
    return (
      <SectionTitle {...props} titleTag={ React.DOM.h1 } className="_page-context">
        {props.children}
      </SectionTitle>
    );
  }
}
