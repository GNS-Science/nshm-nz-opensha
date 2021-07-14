from datetime import datetime as dt
from nshm_toshi_client.toshi_client_base import ToshiClientBase
import copy

class ToshiApi(ToshiClientBase):

    def __init__(self, url, s3_url, auth_token, with_schema_validation=True, headers=None ):
        super(ToshiApi, self).__init__(url, auth_token, with_schema_validation, headers)
        self._s3_url = s3_url

    def OLD_get_general_task_subtask_files(self, id):
        raise("Don't use this, its too slow, use ")
        qry = '''
            query one_general ($id:ID!)  {
              node(id: $id) {
                __typename
                ... on GeneralTask {
                  title
                  description
                  created
                  children {
                    total_count
                    edges {
                      node {
                        child {
                          __typename
                          ... on Node {
                            id
                          }
                          ... on RuptureGenerationTask {
                            created
                            state

                            result
                            arguments {k v}
                            files {
                              total_count
                              edges {
                                node {
                                  role
                                  file {
                                    ... on File {
                                      id
                                      file_name
                                      file_size
                                      meta {k v}
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }'''

        # print(qry)
        input_variables = dict(id=id)
        executed = self.run_query(qry, input_variables)
        return executed

    def get_general_task_subtask_files(self, id):
        return self.get_subtask_files(id)

    def get_subtask_files(self, id):
        gt = self.get_general_task_subtasks(id)
        for subtask in gt['children']['edges']:
            sbt = self.get_rgt_files(subtask['node']['child']['id'])
            subtask['node']['child']['files'] = copy.deepcopy(sbt['files'])
            #TESTING
            #break
        return gt

    def get_general_task_subtasks(self, id):
        qry = '''
            query one_general ($id:ID!)  {
              node(id: $id) {
                __typename
                ... on GeneralTask {
                  id
                  title
                  description
                  created
                  children {
                    #total_count
                    edges {
                      node {
                        child {
                          __typename
                          ... on Node {
                            id
                          }
                          ... on RuptureGenerationTask {
                            created
                            state
                            result
                            arguments {k v}
                          }
                        }
                      }
                    }
                  }
                }
              }
            }'''

        # print(qry)
        input_variables = dict(id=id)
        executed = self.run_query(qry, input_variables)
        return executed['node']

    def get_rgt_files(self, id):

        qry = '''
            query ($id:ID!) {
              node(id: $id) {
                __typename
                ... on RuptureGenerationTask {
                  id
                  files {
                    total_count
                    edges {
                      node {
                        ... on FileRelation {
                          role
                          file {
                            ... on File {
                              id
                              file_name
                              file_size
                              meta {k v}
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
        '''

        # print(qry)
        input_variables = dict(id=id)
        executed = self.run_query(qry, input_variables)
        return executed['node']

    def get_file_detail(self, id):
        qry = '''
        query file ($id:ID!) {
                node(id: $id) {
            __typename
            ... on File {
              id
              file_name
              file_size
              meta {k v}
            }
          }
        }'''

        print(qry)
        input_variables = dict(id=id)
        executed = self.run_query(qry, input_variables)
        return executed['node']


    def get_file_download_url(self, id):
        qry = '''
        query download_file ($id:ID!) {
                node(id: $id) {
            __typename
            ... on File {
              id
              file_name
              file_size
              file_url
            }
          }
        }'''

        print(qry)
        input_variables = dict(id=id)
        executed = self.run_query(qry, input_variables)
        return executed['node']



    def create_table(self, rows, column_headers, column_types, object_id, table_name, created=None):

        created = created or dt.utcnow().isoformat() + 'Z'

        rowlen = len(column_headers)
        assert len(column_types) == rowlen
        for t in column_types:
            assert t in "string,double,integer,boolean".split(',')
        for row in rows:
            assert len(row) == rowlen
            #when do we check the coercions??

        input_variables = {
          "headers": column_headers,
          "object_id": object_id,
          "rows": rows,
          "column_types": column_types,
          "table_name": table_name,
          "created": created
        }

        qry = '''
        mutation create_table ($rows: [[String]]!, $object_id: ID!, $table_name: String!, $headers: [String]!, $column_types: [RowItemType]!, $created: DateTime!) {
          create_table(input: {
            name: $table_name
            created: $created
            object_id: $object_id
            column_headers: $headers
            column_types: $column_types
            rows: $rows
            })
          {
            table {
              id
            }
          }
        }'''

        #print(qry)
        executed = self.run_query(qry, input_variables)
        return executed['create_table']['table']
