#define _GNU_SOURCE

#include "ftkcustomatkobject.h"

void
ftk_custom_atk_object_class_init (AtkObjectClass *klass)
{

  GtkAccessibleClass *accessible_class;
  
  accessible_class = (GtkAccessibleClass *)klass;

  klass->get_n_children = ftk_custom_atk_object_get_n_children;
  klass->ref_child = ftk_custom_atk_object_ref_child;
  klass->initialize = ftk_custom_atk_object_init;
  
}

AtkObject*
ftk_custom_atk_object_new (GtkDrawingArea* widget)
{
  AtkObject *accessible;
  
  accessible = g_object_new (ftk_custom_atk_object_get_type (), NULL);
  g_return_val_if_fail (G_IS_OBJECT (accessible), NULL);

  g_object_ref (accessible);
  return accessible;
}

void ftk_custom_atk_object_init(AtkObject* atkAccessible, gpointer data){
  
  //  atk_object_initialize (atkAccessible, (GObject*) data);
  g_return_if_fail (G_IS_OBJECT (atkAccessible));

  ((FtkCustomAtkObject*)atkAccessible)->n_children = 0;
  ((FtkCustomAtkObject*)atkAccessible)->start_index = 0;
  
  atkAccessible->role = ATK_ROLE_UNKNOWN;

}

GType ftk_custom_atk_object_get_type (void)
{
   
  static GType type = 0;

  if (!type)
    {
      static const GTypeInfo tinfo =
        {
          sizeof (FtkCustomAtkObjectClass),
          (GBaseInitFunc) NULL, /* base init */
          (GBaseFinalizeFunc) NULL, /* base finalize */
          (GClassInitFunc) ftk_custom_atk_object_class_init, /* class init */
          (GClassFinalizeFunc) NULL, /* class finalize */
          NULL, /* class data */
          sizeof (FtkCustomAtkObject), /* instance size */
          0, /* nb preallocs */
          (GInstanceInitFunc) ftk_custom_atk_object_init, /* instance init */
          NULL /* value table */
        };

      type = g_type_register_static (ATK_TYPE_OBJECT,
                                     "FtkCustomAtkObject", &tinfo, 0);
    }

  return type;
}


void       ftk_custom_atk_object_set_n_children(FtkCustomAtkObject* object, int n){
  object->n_children = n;
}

void       ftk_custom_atk_object_set_start_index(FtkCustomAtkObject* object,int i){
  object-> start_index = i;
}
