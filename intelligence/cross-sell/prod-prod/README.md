# cross-sell
## KNN product-product recommender
Generates a product similarity matrix for products based on "customers who
purchased this also purchased that".

### TODO:
- Dockerize it
- only send the top `k` suggestions
- add provisioning entries
- create an activity consumer to train the recommender

### Dependencies
- python3
  - `brew install python3` or `sudo apt-get install python3`
- numpy, scipy, Flask, matplotlib
  - `pip3 install --upgrade pip`
  - `pip3 install numpy scipy Flask matplotlib`
