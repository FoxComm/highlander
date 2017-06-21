import React from 'react';
import PropTypes from 'prop-types';
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
    color: '#f00',
  },
  heading: {
    marginTop: 7,
    fontFamily: font,
    fontWeight: 'bold',
  },
});

export class ComponentsListRenderer extends React.Component {
  state = {
    expanded: {},
  };

  expand(slug) {
    return e => {
      if (this.state.expanded[slug]) {
        e.preventDefault();
      }

      const newExpanded = {
        ...this.state.expanded,
        [slug]: !this.state.expanded[slug],
      };

      this.setState({ expanded: newExpanded });
    };
  }

  render() {
    let { classes, items } = this.props;
    items = items.filter(item => item.name);

    if (!items.length) {
      return null;
    }

    console.log(items);

    const activeItem = window.location.hash.substr(2);
    console.log(activeItem);

    return (
      <ul className={classes.list}>
        {items.map(({ heading, name, slug, content }) =>
          <li className={cx(classes.item, (!content || !content.props.items.length) && classes.isChild)} key={name}>
            <Link
              className={cx(heading && classes.heading, activeItem === slug && classes.isActive)}
              href={`/#${slug}`}
              onClick={!!content && this.expand(slug)}
            >
              {name}
            </Link>
            {this.state.expanded[slug] && content}
          </li>
        )}
      </ul>
    );
  }
}

ComponentsListRenderer.propTypes = {
  items: PropTypes.array.isRequired,
  classes: PropTypes.object.isRequired,
};

export default Styled(styles)(ComponentsListRenderer);
