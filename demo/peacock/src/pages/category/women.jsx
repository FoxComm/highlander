// @flow

import React from 'react';
import Button, { SecondaryButton } from 'ui/buttons';
import { Link } from 'react-router';
import LocalNav from 'components/local-nav/local-nav';

import styles from './women.css';

const navItems = [
  { label: 'Apparel', to: '/s/women/apparel' },
  { label: 'Shoes', to: '/s/women/shoes' },
  { label: 'Accessories', to: '/s/women/accessories' },
  { label: 'View All', to: '/s/women' },
];

const WomensCatPage = () => {
  return (
    <div>
      <LocalNav categoryName="Women" links={navItems} />
      <div styleName="wrap">
        <div styleName="header-wrap">
          <Link styleName="header-content" to="/s/women/training">
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
          </Link>
        </div>
        <div styleName="featured">
          <div styleName="featured-card">
            <Link to="/search/climachill">
              <div styleName="climachill">
                <div>Climachill</div>
                <div styleName="featured-button">
                  <SecondaryButton>
                    Shop now
                  </SecondaryButton>
                </div>
              </div>
            </Link>
          </div>
          <div styleName="featured-card">
            <Link to="/search/reigning">
              <div styleName="champ">
                <div>Athletics<br />X Reigning Champ</div>
                <div styleName="featured-button">
                  <SecondaryButton>
                    Shop now
                  </SecondaryButton>
                </div>
              </div>
            </Link>
          </div>
          <div styleName="featured-card">
            <Link to="/s/women/originals">
              <div styleName="originals">
                <div>Originals:<br />Ocean Elements</div>
                <div styleName="featured-button">
                  <SecondaryButton>
                    Shop now
                  </SecondaryButton>
                </div>
              </div>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default WomensCatPage;
