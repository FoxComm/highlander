/* @flow */

import React from 'react';
import styles from './footer.css';
import type { HTMLElement } from 'types';

import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

import Icon from 'ui/icon';

const Footer = (props: Localized) : HTMLElement => {
  const { t } = props;

  return (
    <div styleName="container">
      <div styleName="social-links">
        <Icon name="fc-instagram" styleName="social-icon" />
        <Icon name="fc-facebook" styleName="social-icon" />
        <Icon name="fc-twitter" styleName="social-icon" />
        <Icon name="fc-pinterest" styleName="social-icon" />
      </div>
      <div styleName="other-links">
        <a href="#" styleName="other-link">{t('Terms of Use')}</a>
        <a href="#" styleName="other-link">{t('Privacy Policy')}</a>
        <a href="#" styleName="other-link">{t('Shipping & Returns')}</a>
      </div>
    </div>
  );
};

export default localized(Footer);
