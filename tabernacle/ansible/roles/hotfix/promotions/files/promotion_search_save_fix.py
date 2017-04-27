#!/usr/bin/env python
from __future__ import print_function

import os
import sys
import json
import urllib2
import psycopg2

# CONN_STR = "dbname='phoenix_development' user='phoenix' host='localhost'"
# PHOENIX_URL = 'http://localhost:9090'

CONN_STR = os.environ['DB_CONN_STRING']
PHOENIX_URL = os.environ['PHOENIX_URL']

JWT = os.environ['JWT'].strip()


sqlFindBugDiscounts = """select
  q.id,
  q.form_id,
  q.offer,
  q.qualifier,
  q.context
    from
  (select
    d.id,
    f.id as form_id,
    c.name as context,
    illuminate_obj(f, s, 'offer') as offer,
    illuminate_obj(f, s, 'qualifier') as qualifier
  from discounts as d
    inner join object_forms as f on (f.id = d.form_id)
    inner join object_contexts as c on (c.id = d.context_id)
    inner join object_shadows as s on (s.id = d.shadow_id)) as q
  where q.offer::text like '%references%' or q.qualifier::text like '%references%';"""


# [
# (56, 201, 
# {u'itemsPercentOff': {u'discount': 15, u'references': [{u'referenceType': u'SavedProductSearch', u'referenceId': 4}]}}, 
# {u'itemsAny': {u'references': [{u'referenceType': u'SavedProductSearch', u'referenceId': 4}]}})]

def do_phoenix_req(path, method = None, data = None):
    url = "%s/%s" % (PHOENIX_URL, path)
    req = urllib2.Request(url)
    req.add_header('JWT', JWT)

    if method is None:
        method = 'GET'
    elif data is not None:
        req.add_header('Content-Type', 'application/json')

    print('\tmake requests: %s %s' % (method, url))

    if not isinstance(data, (str, unicode)):
        data = json.dumps(data)

    req.get_method = lambda: method

    return urllib2.urlopen(req, data)

def get_discount(id, context):
    uri = 'v1/discounts/%s/%d/baked' % (context, id)
    resp = do_phoenix_req(uri)
    if resp.getcode() != 200:
        return

    return json.loads(resp.read())

def update_discount(id, context, new_discount):
    uri = 'v1/discounts/%s/%d' % (context, id)
    return do_phoenix_req(uri, 'PATCH', new_discount).getcode()

def phoenix_fix_discount(form_id, context, new_offer, new_qualifier):
    discount = get_discount(form_id, context)
    discount['attributes']['offer']['v'] = new_offer
    discount['attributes']['qualifier']['v'] = new_qualifier
    update_discount(form_id, context, discount)


def convert_search(search):
    refType = search.get('referenceType')
    if refType != 'SavedProductSearch':
        return
    refId = search.get('referenceId')
    if refId is None:
        return
    return {'productSearchId': refId}

def fix_search_reference(did, ref, subtype):
    print('Make valid %s for discount=%d' % (subtype, did))
    newRef = {}
    for k, val in ref.iteritems():
        old_searches = val.get('references')
        new_searches = []

        if old_searches is None:
            print('references not found')
            return

        for old_search in old_searches:
            new_search = convert_search(old_search)
            if new_search is None:
                return
            new_searches.append(new_search)

        
        newRef[k] = val
        del newRef[k]['references']
        newRef[k]['search'] = new_searches

    return newRef


def doFix():
    conn = psycopg2.connect(CONN_STR)
    cur = conn.cursor()
    cur.execute(sqlFindBugDiscounts)
    rows = cur.fetchall()
    for row in rows:
        (id, form_id, offer, qualifier, context) = row
        print('Try to fix discount %d (form_id=%d)' % (id, form_id))

        new_offer = fix_search_reference(id, offer, 'offer')
        new_qualifier = fix_search_reference(id, qualifier, 'qualifier')

        if not new_offer or not new_qualifier:
            println("Can't fix discount %d, skip" % id)
            continue

        print('new_offer = %r' % new_offer)
        print('new_qualifier = %r' % new_qualifier)

        phoenix_fix_discount(form_id, context, new_offer, new_qualifier)

    cur.close()
    conn.close()

if __name__ == "__main__":
    doFix()
