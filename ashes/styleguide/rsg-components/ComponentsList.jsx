import React from 'react';
import PropTypes from 'prop-types';
import { identity, values } from 'lodash';
import { assoc } from 'sprout-data';
import cx from 'classnames';
import Link from 'rsg-components/Link';
import Styled from 'rsg-components/Styled';

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

function isLeaf(item) {
  return !item.content || !item.content.props.items.length;
}

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

function updateHash(hash) {
  if (history.pushState) {
    history.pushState(null, null, `#/${hash}`);
  } else {
    location.hash = `#/${hash}`;
  }
}

class ListItem extends React.Component {
  _content;
  mounted: bool;

  componentDidMount() {
    this.mounted = true;

    this.recalculateHeight();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  componentDidUpdate() {
    this.recalculateHeight();
  }

  recalculateHeight() {
    if (!this.props.collapsible) {
      this._content.style.opacity = 1;
      this._content.style.overflow = 'visible';

      return;
    }

    let maxHeight = 0;
    let opacity = 0;

    if (this.props.open) {
      maxHeight = this._content.scrollHeight;
      opacity = 1;
    }

    this._content.style.maxHeight = `${maxHeight}px`;
    this._content.style.opacity = opacity;
  }

  render() {
    return (
      <div ref={c => (this._content = c)} style={{ transition: 'all .4s', opacity: 0, overflow: 'hidden', }}>
        {React.cloneElement(this.props.content)}
      </div>
    );
  }
}

export class ComponentsListRenderer extends React.Component {
  state = {
    expandedItems: {},
    collapsibleItems: {},
    hasCollapsibleItems: false,
  };

  componentWillMount() {
    const collapsibleItems = this.props.items.reduce(
      (res, item) => ({
        ...res,
        [item.slug]: isLeaf(item) ? false : item.content.props.items.some(isLeaf),
      }),
      {}
    );

    const hasCollapsibleItems = values(collapsibleItems).some(identity);

    this.setState({ collapsibleItems, hasCollapsibleItems });
  }

  componentDidMount() {
    if (this.state.hasCollapsibleItems) {
      window.addEventListener('scroll', this.handleScroll.bind(this));
    }
  }

  get activeItem() {
    return window.location.hash.substr(2);
  }

  getExpandedState(slug, expand = true) {
    const newState = {
      ...this.state.expandedItems,
      [slug]: expand,
    };

    return newState;
  }

  handleScroll() {
    if (!this.state.hasCollapsibleItems) {
      return;
    }

    this.props.items.forEach(parent => {
      if (isLeaf(parent)) {
        return;
      }

      const elementInViewport = parent.content.props.items.find(item => {
        if (!isLeaf(item)) {
          return;
        }

        return isElementWithSlugInViewport(item.slug);
      });

      if (elementInViewport) {
        if (this.activeItem !== elementInViewport.slug) {
          updateHash(elementInViewport.slug);

          this.setState({
            expandedItems: this.getExpandedState(parent.slug, true),
          });
        }
      } else {
        this.setState({
          expandedItems: this.getExpandedState(parent.slug, false),
        });
      }
    });
  }

  expand(slug) {
    return e => {
      const itemExpanded = this.state.expandedItems[slug];

      if (this.state.collapsibleItems[slug]) {
        e.preventDefault();
      }

      this.setState({ expandedItems: this.getExpandedState(slug, !itemExpanded) });
    };
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
          const { heading, name, slug, content } = item;
          const isLeafElement = isLeaf(item);
          const activeParent = !isLeafElement && content.props.items.some(({ slug }) => slug === this.activeItem);

          const cls = cx(classes.item, {
            [classes.isChild]: isLeafElement,
            [classes.isActive]: isLeafElement && this.activeItem === slug,
            [classes.activeParent]: activeParent,
          });

          const renderContent = !this.state.collapsibleItems[slug] || this.state.expandedItems[slug] || activeParent;

          const TitleElement = isLeafElement || this.state.collapsibleItems[slug] ? Link : 'span';
          const clickHandler = isLeafElement || this.state.collapsibleItems[slug] ? this.expand(slug) : void 0;

          return (
            <li className={cls} key={slug}>
              <TitleElement className={cx({ [classes.heading]: !!heading })} href={`#${slug}`} onClick={clickHandler}>
                {name}
              </TitleElement>
              {!!content &&
              <ListItem open={renderContent} content={content} collapsible={this.state.collapsibleItems[slug]} />}
            </li>
          );
        })}
      </ul>
    );
  }
}

ComponentsListRenderer.propTypes = {
  items: PropTypes.array.isRequired,
  classes: PropTypes.object.isRequired,
};

export default Styled(styles)(ComponentsListRenderer);
