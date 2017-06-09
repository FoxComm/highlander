```
const container = { 
  display: 'flex',
  flexWrap: 'wrap',
  justifyContent: 'center',
};

const box =  {
  display: 'flex',
  flexDirection: 'column',
  margin: '10px',
  color: '#6d859e'
}

const iconWrapper =  {
  width: '120px',
  height: '120px',
  textAlign: 'center',
  lineHeight: '120px',
  fontSize: '28px',
  backgroundColor: '#6D859E'
  
}

const text = {
  marginTop: '5px',
  fontSize: '12px',
  textAlign: 'center',
  lineHeight: '15px'
}

const names = [
  "applications",
  "carts",
  "categories",
  "channels",
  "customers",
  "gift-cards",
  "groups",
  "orders",
  "plugins",
  "products",
  "promotions",
  "skus",
  "tags",
  "taxonomies",
];

const svgStyle = {
  fill: '#6d859e',
  margin: '42px'
}

const SvgIconography = () => {
    const icons = names.map((name) => (
    <div key={name} style={box}>
      <div style={iconWrapper}> 
        <svg style={svgStyle} viewBox='0 0 19 19'>
          <use xlinkHref={`#icon-${name}`} />
        </svg>
      </div>
      <span style={text}>{name}</span>
    </div>
  ));

  return(
    <div style={container}>
      {icons}
    </div>
  );
};

<SvgIconography />
```
