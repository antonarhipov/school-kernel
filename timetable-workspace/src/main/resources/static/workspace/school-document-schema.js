// JSON Schema for editable school data. Unfinished numeric input is retained as text.
export const schoolDocumentSchema = {
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://school-kernel.invalid/schema/school-document-v1.schema.json",
  "title": "Complete School Setup document v1",
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "documentType": {
      "const": "school-setup"
    },
    "formatVersion": {
      "const": 1
    },
    "name": {
      "type": "string"
    },
    "grid": {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "activeDays": {
          "type": "array",
          "items": {
            "enum": [
              "MONDAY",
              "TUESDAY",
              "WEDNESDAY",
              "THURSDAY",
              "FRIDAY",
              "SATURDAY",
              "SUNDAY"
            ]
          }
        },
        "intervals": {
          "type": "array",
          "items": {
            "type": "object",
            "additionalProperties": false,
            "properties": {
              "id": {
                "type": "string"
              },
              "periodId": {
                "type": "string"
              },
              "day": {
                "enum": [
                  "MONDAY",
                  "TUESDAY",
                  "WEDNESDAY",
                  "THURSDAY",
                  "FRIDAY",
                  "SATURDAY",
                  "SUNDAY"
                ]
              },
              "kind": {
                "enum": [
                  "Lesson",
                  "Reserved lesson",
                  "Break",
                  "Lunch"
                ]
              },
              "label": {
                "type": "string"
              },
              "start": {
                "type": "string"
              },
              "end": {
                "type": "string"
              },
              "order": {
                "type": [
                  "integer",
                  "string"
                ]
              },
              "autoLabel": {
                "type": "boolean"
              },
              "unspecifiedTimes": {
                "type": "boolean"
              },
              "boundaryAfter": {
                "type": "boolean"
              }
            },
            "required": [
              "id",
              "day",
              "kind",
              "label",
              "start",
              "end"
            ]
          }
        }
      },
      "required": [
        "activeDays",
        "intervals"
      ]
    },
    "subjects": {
      "type": "array",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "id": {
            "type": "string"
          },
          "displayName": {
            "type": "string"
          },
          "reservedPeriodsAllowed": {
            "type": "boolean"
          },
          "maxWeeklyReservedLessonsPerCohort": {
            "type": [
              "integer",
              "string"
            ]
          },
          "dayEdgeOnly": {
            "type": "boolean"
          },
          "maxDailyLessonsPerCohort": {
            "type": [
              "integer",
              "string"
            ]
          },
          "curatorLesson": {
            "type": "boolean"
          },
          "name": {
            "type": "string"
          }
        },
        "required": [
          "id"
        ]
      }
    },
    "teachers": {
      "type": "array",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "id": {
            "type": "string"
          },
          "displayName": {
            "type": "string"
          },
          "qualifiedSubjectIds": {
            "type": "array",
            "items": {
              "type": "string"
            }
          },
          "availablePeriodIds": {
            "type": "array",
            "items": {
              "type": "string"
            }
          },
          "undesirablePeriodIds": {
            "type": "array",
            "items": {
              "type": "string"
            }
          },
          "name": {
            "type": "string"
          },
          "availability": {
            "type": "object",
            "additionalProperties": false,
            "properties": {
              "intent": {
                "enum": [
                  "all",
                  "whitelist"
                ]
              },
              "availablePeriodIds": {
                "type": "array",
                "items": {
                  "type": "string"
                }
              },
              "unavailablePeriodIds": {
                "type": "array",
                "items": {
                  "type": "string"
                }
              }
            },
            "required": [
              "intent"
            ]
          },
          "eligibleClassIdsBySubject": {
            "type": "object",
            "additionalProperties": {
              "type": "array",
              "items": {
                "type": "string"
              }
            }
          }
        },
        "required": [
          "id"
        ]
      }
    },
    "classes": {
      "type": "array",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "id": {
            "type": "string"
          },
          "displayName": {
            "type": "string"
          },
          "size": {
            "type": [
              "integer",
              "string"
            ]
          },
          "availablePeriodIds": {
            "type": "array",
            "items": {
              "type": "string"
            }
          },
          "undesirablePeriodIds": {
            "type": "array",
            "items": {
              "type": "string"
            }
          },
          "maxDailyLessonSpread": {
            "type": [
              "integer",
              "string"
            ]
          },
          "maxDailyGaps": {
            "type": [
              "integer",
              "string"
            ]
          },
          "latestStartSlot": {
            "type": [
              "integer",
              "string"
            ]
          },
          "preferredLatestStartSlot": {
            "type": [
              "integer",
              "string"
            ]
          },
          "dailyLessonSpreadLimit": {
            "type": [
              "integer",
              "string"
            ]
          },
          "curatorTeacherId": {
            "type": "string"
          },
          "homeRoomId": {
            "type": "string"
          },
          "name": {
            "type": "string"
          },
          "availability": {
            "type": "object",
            "additionalProperties": false,
            "properties": {
              "intent": {
                "enum": [
                  "all",
                  "whitelist"
                ]
              },
              "availablePeriodIds": {
                "type": "array",
                "items": {
                  "type": "string"
                }
              },
              "unavailablePeriodIds": {
                "type": "array",
                "items": {
                  "type": "string"
                }
              }
            },
            "required": [
              "intent"
            ]
          },
          "lunchPeriodIds": {
            "type": "array",
            "items": {
              "type": "string"
            }
          }
        },
        "required": [
          "id"
        ]
      }
    },
    "rooms": {
      "type": "array",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "id": {
            "type": "string"
          },
          "displayName": {
            "type": "string"
          },
          "capacity": {
            "type": [
              "integer",
              "string"
            ]
          },
          "capabilityIds": {
            "type": "array",
            "items": {
              "type": "string"
            }
          },
          "availablePeriodIds": {
            "type": "array",
            "items": {
              "type": "string"
            }
          },
          "name": {
            "type": "string"
          },
          "availability": {
            "type": "object",
            "additionalProperties": false,
            "properties": {
              "intent": {
                "enum": [
                  "all",
                  "whitelist"
                ]
              },
              "availablePeriodIds": {
                "type": "array",
                "items": {
                  "type": "string"
                }
              },
              "unavailablePeriodIds": {
                "type": "array",
                "items": {
                  "type": "string"
                }
              }
            },
            "required": [
              "intent"
            ]
          }
        },
        "required": [
          "id"
        ]
      }
    },
    "plans": {
      "type": "array",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "id": {
            "type": "string"
          },
          "classId": {
            "type": "string"
          },
          "cohortId": {
            "type": "string"
          },
          "subjectId": {
            "type": "string"
          },
          "requiredCount": {
            "type": [
              "integer",
              "string"
            ]
          },
          "allocations": {
            "type": "array",
            "items": {
              "type": "object",
              "additionalProperties": false,
              "properties": {
                "teacherId": {
                  "type": "string"
                },
                "count": {
                  "type": [
                    "integer",
                    "string"
                  ]
                }
              },
              "required": [
                "teacherId",
                "count"
              ]
            }
          },
          "occurrences": {
            "type": "array",
            "items": {
              "type": "object",
              "additionalProperties": false,
              "properties": {
                "id": {
                  "type": "string"
                },
                "displayName": {
                  "type": "string"
                },
                "teacherId": {
                  "type": "string"
                },
                "seriesId": {
                  "type": "string"
                },
                "requiredRoomCapabilityIds": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                "preferredRoomIds": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                "undesirablePeriodIds": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                "periodLock": {
                  "type": "string"
                },
                "roomLock": {
                  "type": "string"
                },
                "protected": {
                  "type": "boolean"
                },
                "exception": {
                  "type": [
                    "string",
                    "boolean",
                    "object"
                  ]
                }
              },
              "required": [
                "id"
              ]
            }
          },
          "blockRules": {
            "type": "array",
            "items": {
              "type": "object",
              "additionalProperties": false,
              "properties": {
                "id": {
                  "type": "string"
                },
                "length": {
                  "type": [
                    "integer",
                    "string"
                  ]
                },
                "operator": {
                  "enum": [
                    "EXACTLY",
                    "AT_LEAST"
                  ]
                },
                "count": {
                  "type": [
                    "integer",
                    "string"
                  ]
                }
              },
              "required": [
                "id",
                "length",
                "operator",
                "count"
              ]
            }
          },
          "seriesId": {
            "type": "string"
          },
          "nextOccurrence": {
            "type": "integer",
            "minimum": 0
          },
          "occurrenceTemplate": {
            "type": "object",
            "additionalProperties": false,
            "properties": {
              "seriesId": {
                "type": "string"
              },
              "requiredRoomCapabilityIds": {
                "type": "array",
                "items": {
                  "type": "string"
                }
              },
              "preferredRoomIds": {
                "type": "array",
                "items": {
                  "type": "string"
                }
              },
              "undesirablePeriodIds": {
                "type": "array",
                "items": {
                  "type": "string"
                }
              },
              "periodLock": {
                "type": "string"
              },
              "roomLock": {
                "type": "string"
              },
              "protected": {
                "type": "boolean"
              },
              "exception": {
                "type": [
                  "string",
                  "boolean",
                  "object"
                ]
              }
            },
            "required": []
          },
          "fixedRoomId": {
            "type": "string"
          },
          "protected": {
            "type": "boolean"
          },
          "protectedReasons": {
            "type": "array",
            "items": {
              "type": "string"
            }
          },
          "blockEditsProtected": {
            "type": "string"
          }
        },
        "required": [
          "id"
        ]
      }
    },
    "roomAssignments": {
      "type": "array",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "properties": {
          "id": {
            "type": "string"
          },
          "subjectId": {
            "type": "string"
          },
          "teacherId": {
            "type": "string"
          },
          "allowedRoomIds": {
            "type": "array",
            "items": {
              "type": "string"
            }
          },
          "useHomeRoom": {
            "const": true
          }
        },
        "required": [
          "id"
        ]
      }
    },
    "kernelSchoolId": {
      "type": "string"
    },
    "kernelExtras": {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "softConstraintOverrides": {
          "type": "array",
          "items": {
            "type": "object",
            "additionalProperties": false,
            "properties": {
              "constraintId": {
                "enum": [
                  "soft.teacher-gap",
                  "soft.cohort-gap",
                  "soft.cohort-late-start",
                  "soft.cohort-week-balance",
                  "soft.series-same-day",
                  "soft.undesirable-period",
                  "soft.non-preferred-room"
                ]
              },
              "weight": {
                "type": [
                  "integer",
                  "string"
                ]
              }
            }
          }
        }
      }
    },
    "kernelCollectionPresence": {
      "type": "array",
      "items": {
        "enum": [
          "reservedPeriodIds",
          "consecutiveBlocks"
        ]
      }
    }
  },
  "required": [
    "documentType",
    "formatVersion",
    "name",
    "grid",
    "subjects",
    "teachers",
    "classes",
    "rooms",
    "plans"
  ]
};
