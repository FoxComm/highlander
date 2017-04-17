// @flow

import React from 'react';
import Button from 'ui/buttons';
import { Link } from 'react-router';
import LocalNav from 'components/local-nav/local-nav';

import styles from './women.css';

type Props = {};

const navItems = [
  { label: 'Apparel', to: '/s/women/apparel' },
  { label: 'Shoes', to: '/s/women/shoes' },
  { label: 'Accessories', to: '/s/women/accessories' },
  { label: 'View All', to: '/s/women' },
];

const WomensCatPage = (props: Props) => {
  return (
    <div>
      <LocalNav categoryName="Women" links={navItems} />
      <div styleName="wrap">
        <div styleName="header-wrap">
          <div styleName="header-content">
            <div styleName="header-title">
              Karlie Is Never Done
            </div>
            <div styleName="header-body">
              <p>
                The latest Training Collection is all about breaking boundaries.
              </p>
              <div styleName="header-button">
                <Button>
                  Shop now
                </Button>
              </div>
            </div>
          </div>
        </div>
        <div styleName="featured">
          <div styleName="featured-card">
            <div styleName="climachill">
              <div>Climachill</div>
              <div>
                <Button>
                  Shop now
                </Button>
              </div>
            </div>
          </div>
          <div styleName="featured-card">
            <div styleName="champ">
              <div>Athletics X Reigning Champ</div>
              <div>
                <Button>
                  Shop now
                </Button>
              </div>
            </div>
          </div>
          <div styleName="featured-card">
            <div styleName="originals">
              <div>Originals: Ocean Elements</div>
              <div>
                <Button>
                  Shop now
                </Button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default WomensCatPage;
