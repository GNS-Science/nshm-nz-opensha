
from nshm_toshi_client.toshi_client_base import ToshiClientBase

class ToshiApi(ToshiClientBase):

    def __init__(self, url, s3_url, auth_token, with_schema_validation=True, headers=None ):
        super(ToshiApi, self).__init__(url, auth_token, with_schema_validation, headers)
        self._s3_url = s3_url

    def get_general_task_subtask_files(self, id):
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
