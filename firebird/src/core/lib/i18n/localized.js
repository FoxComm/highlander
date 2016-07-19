import React, { Component, PropTypes } from 'react';
import Gettext from 'node-gettext';
import { autobind } from 'core-decorators';


type LocalizedProps = {
  text: string;
  plural: ?string;
  count: ?number;
};

type LocalizedContext = {
  translator: Gettext;
};


export default function localized(WrappedComponent) {
  return class Localized extends Component {

    props: LocalizedProps;

    context: LocalizedContext;

    static contextTypes = {
      translator: PropTypes.object.isRequired,
    };

    @autobind
    t(message, plural, count) {
      if (this.context.translator.textdomain() !== 'en' &&
        !this.context.translator._getTranslation(this.context.translator.textdomain(), '', message)) {
        console.error('No translation found for', message);
      }

      return this.context.translator.ngettext(message, plural, count);
    }

    render() {
      return React.createElement(WrappedComponent, { t: this.t, ...this.props });
    }

  };
}
