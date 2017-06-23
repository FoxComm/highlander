import React from 'react';
import PropTypes from 'prop-types';
import { trim } from 'lodash';
import cx from 'classnames';
import { autobind } from 'core-decorators';
import Link from 'rsg-components/Link';
import Styled from 'rsg-components/Styled';
import ListItem from './ComponentsListItem';

require('smoothscroll-polyfill').polyfill();

const styles = ({ font, small }) => ({
  list: {
    margin: 0,
    paddingLeft: 15,
  },
  item: {
    display: 'block',
    margin: [[7, 0, 7, 0]],
    fontFamily: font,
    fontSize: 15,
    listStyle: 'none',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
  },
  isChild: {
    [small]: {
      display: 'inline-block',
      margin: [[0, 7, 0, 0]],
    },
  },
  isActive: {
    opacity: 1,
  },
  activeParent: {
    opacity: 1,
  },
  heading: {
    marginTop: 7,
    fontFamily: font,
    fontWeight: 'bold',
  },
});

function isElementWithSlugInViewport(slug) {
  const el = document.getElementById(slug);
  if (!el) {
    console.warn(`element "${slug}" not found`);

    return false;
  }
  const elementTop = el.offsetTop;
  const elementBottom = elementTop + el.offsetHeight;
  const pageTop = window.scrollY;

  return elementTop + 50 <= pageTop + 200 && elementBottom >= pageTop;
}

export class ComponentsListRenderer extends React.Component {
  static defaultProps = {
    expanded: false,
  }

  isActiveSlug(slug) {
    return slug === window.location.hash.substr(2);
  }

  render() {
    let { classes, items } = this.props;
    items = items.filter(item => item.name);

    if (!items.length) {
      return null;
    }

    return (
      <ul className={classes.list}>
        {items.map(item => {
          const { heading, name, slug, content, collapsible, expanded, isLeaf } = item;
          const activeParent = !isLeaf && content.props.items.some(({ slug }) => this.isActiveSlug(slug));

          const cls = cx(classes.item, {
            [classes.isChild]: isLeaf,
            [classes.isActive]: isLeaf && this.isActiveSlug(slug),
            [classes.activeParent]: activeParent,
          });

          const TitleElement = isLeaf || collapsible ? Link : 'span';

          return (
            <li className={cls} key={slug}>
              <TitleElement
                className={cx({ [classes.heading]: !!heading })}
                href={`#${slug}`}
                onClick={e => this.props.onItemClick(slug, e)}
              >
                {name}
              </TitleElement>
              {!!content && <ListItem open={expanded || activeParent} content={content} collapsible={collapsible} />}
            </li>
          );
        })}
      </ul>
    );
  }
}

ComponentsListRenderer.propTypes = {
  items: PropTypes.array.isRequired,
  level: PropTypes.number.isRequired,
  isLeaves: PropTypes.bool.isRequired,
  expanded: PropTypes.bool.isRequired,
  onItemClick: PropTypes.func.isRequired,
  classes: PropTypes.object.isRequired,
};

export default Styled(styles)(ComponentsListRenderer);
