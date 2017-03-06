import _ from 'lodash';
import { Component, PropTypes } from 'react';
import Gettext from 'node-gettext';
import plurals from 'node-gettext/lib/plurals';


export default class Provider extends Component {

  static propTypes = {
    locale: PropTypes.string.isRequired,
    translation: PropTypes.object.isRequired,
    children: PropTypes.node,
  };

  static childContextTypes = {
    translator: PropTypes.object.isRequired,
  };

  constructor(props, context) {
    super(props, context);

    this.translator = new Gettext();
    this.updateTranslator(props);
  }

  componentWillReceiveProps(props) {
    this.updateTranslator(props);
  }

  updateTranslator({locale, translation}) {
    if (!locale || !translation) {
      return;
    }

    const data = _.clone(translation);
    const pluralsInfo = plurals[locale];
    data.pluralsFunc = pluralsInfo ? pluralsInfo.pluralsFunc : plurals.en.pluralsFunc;

    this.translator.domains[locale] = data;

    this.translator.textdomain(locale);
  }

  getChildContext() {
    return {
      translator: this.translator,
    };
  }

  render() {
    return this.props.children;
  }
}
